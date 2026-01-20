package com.ecosystem.service;

import com.ecosystem.dto.punchout.*;
import com.ecosystem.entity.User;
import com.ecosystem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Punchout Service - 处理punchout相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PunchoutService {

    private final UserRepository userRepository;
    private final PunchoutAuthenticationService authenticationService;
    private final PunchOutOrderMessageBuilder orderMessageBuilder;
    private final PunchoutReturnClient returnClient;

    @Value("${punchout.frontend-url:http://localhost:3000}")
    private String frontendUrl;
    
    @Value("${punchout.supplier-identity:Allinton123}")
    private String supplierIdentity;
    
    @Value("${punchout.supplier-domain:allinton.com.sg}")
    private String supplierDomain;
    
    @Value("${punchout.allowed-return-hosts:}")
    private String allowedReturnHostsConfig;
    
    // 允许的returnUrl host列表（SSRF防护）
    private final Set<String> allowedReturnHosts = new HashSet<>();
    
    /**
     * 初始化允许的returnUrl hosts（SSRF防护）
     */
    @PostConstruct
    public void init() {
        // 默认允许的hosts（示例）
        allowedReturnHosts.add("airliquideasia-test.coupahost.com");
        allowedReturnHosts.add("airliquide.coupahost.com");
        
        // 从配置文件加载
        if (allowedReturnHostsConfig != null && !allowedReturnHostsConfig.trim().isEmpty()) {
            String[] hosts = allowedReturnHostsConfig.split(",");
            for (String host : hosts) {
                String trimmedHost = host.trim();
                if (!trimmedHost.isEmpty()) {
                    allowedReturnHosts.add(trimmedHost);
                }
            }
        }
        
        log.info("Initialized allowed return URL hosts: {}", allowedReturnHosts);
    }

    // 临时会话存储（生产环境应该使用Redis或数据库）
    private final Map<String, PunchoutSession> sessionStore = new ConcurrentHashMap<>();

    /**
     * Punchout会话信息
     */
    @SuppressWarnings("unused")
    private static class PunchoutSession {
        String userId;
        String buyerCookie;  // 用于日志和审计
        String buyerId;  // 用于日志和审计
        String buyerName;  // 用于日志和审计
        String organizationId;  // 用于日志和审计
        Instant expiresAt;  // 使用Instant避免时区问题
        String returnUrl;  // BrowserFormPost.URL，用于POST PunchOutOrderMessage
        
        // cXML Header信息（用于构建PunchOutOrderMessage）
        // 注意：不存储senderSharedSecret（敏感信息，只用于认证，不用于业务）
        String buyerIdentity;  // 买方标识（从SetupRequest的From读取）
        String buyerDomain;    // 买方域名（从SetupRequest的From读取）
        String senderIdentity; // 发送者标识（用于审计，不包含SharedSecret）
        
        AtomicInteger returnAttempts = new AtomicInteger(0);  // Return重试次数（线程安全）
        AtomicBoolean returning = new AtomicBoolean(false);  // Return单次提交锁（防止并发重复提交）

        PunchoutSession(String userId, String buyerCookie, String buyerId, String buyerName, 
                       String organizationId, Instant expiresAt, String returnUrl,
                       String buyerIdentity, String buyerDomain, String senderIdentity) {
            this.userId = userId;
            this.buyerCookie = buyerCookie;
            this.buyerId = buyerId;
            this.buyerName = buyerName;
            this.organizationId = organizationId;
            this.expiresAt = expiresAt;
            this.returnUrl = returnUrl;
            this.buyerIdentity = buyerIdentity;
            this.buyerDomain = buyerDomain;
            this.senderIdentity = senderIdentity;
        }

        boolean isExpired() {
            return expiresAt.isBefore(Instant.now());
        }
        
        int incrementReturnAttempts() {
            return returnAttempts.incrementAndGet();
        }
        
        int getReturnAttempts() {
            return returnAttempts.get();
        }
    }

    /**
     * 处理cXML格式的punchout setup请求
     * 
     * @param cxmlRequest cXML格式的请求
     * @return cXML格式的响应
     */
    public String setupPunchoutCxml(CxmlSetupRequest cxmlRequest) {
        log.info("Processing cXML punchout setup request for buyerCookie: {}", cxmlRequest.getBuyerCookie());
        
        // 严格校验协议骨架
        validateProtocolSkeleton(cxmlRequest);
        
        // 认证发送者
        if (!authenticationService.authenticate(cxmlRequest.getSenderIdentity(), cxmlRequest.getSenderSharedSecret())) {
            throw new RuntimeException("Authentication failed for sender: " + cxmlRequest.getSenderIdentity());
        }
        
        // 从Extrinsic中提取用户信息（宽松处理）
        // 保护：extrinsic可能为null
        Map<String, String> extrinsic = Optional.ofNullable(cxmlRequest.getExtrinsic())
            .orElse(Collections.emptyMap());
        String userEmail = extrinsic.getOrDefault("UserEmail", 
            extrinsic.getOrDefault("UniqueName", 
            extrinsic.getOrDefault("User", null)));
        
        String firstName = extrinsic.get("FirstName");
        String lastName = extrinsic.get("LastName");
        String buyerName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "").trim();
        
        // 查找用户
        User user = findUserByEmail(userEmail);
        if (user == null) {
            // 生产环境：找不到用户就拒绝（避免权限/价格/合同错乱）
            // 开发环境可以fallback，但建议建立buyer->user的映射规则
            log.error("User not found for email: {}. Cannot proceed with punchout setup.", userEmail);
            throw new RuntimeException("User not found for email: " + userEmail + 
                ". Please ensure the user exists or configure buyer-to-user mapping.");
        }
        
        // 生成会话token
        String sessionToken = UUID.randomUUID().toString();
        
        // 创建会话（30分钟过期）
        Instant expiresAt = Instant.now().plusSeconds(1800);  // 30分钟 = 1800秒
        // 注意：SetupRequest的From是Buyer，To是Supplier
        // 我们存储Buyer信息（从From读取），Supplier信息从配置读取
        PunchoutSession session = new PunchoutSession(
            user.getId(),
            cxmlRequest.getBuyerCookie(),
            userEmail,
            buyerName,
            extrinsic.get("BusinessUnit"),
            expiresAt,
            cxmlRequest.getBrowserFormPostUrl(),
            // Buyer信息（从SetupRequest的From读取）
            cxmlRequest.getFromIdentity(),  // Buyer Identity
            cxmlRequest.getFromDomain(),     // Buyer Domain
            // Sender Identity（用于审计，不包含SharedSecret）
            cxmlRequest.getSenderIdentity()
        );
        
        sessionStore.put(sessionToken, session);
        
        // 构建重定向URL（只包含sessionToken，不包含JWT token，避免安全风险）
        String redirectUrl = String.format("%s/punchout/shopping?sessionToken=%s",
            frontendUrl, sessionToken);
        
        log.info("cXML punchout setup successful (requestPayloadID: {}), redirectUrl: {}", 
            cxmlRequest.getPayloadId(), redirectUrl);
        
        // 生成cXML响应（关联request的payloadID）
        return generateCxmlResponse(cxmlRequest, redirectUrl);
    }
    
    /**
     * 严格校验协议骨架
     */
    private void validateProtocolSkeleton(CxmlSetupRequest request) {
        // 必须字段
        if (request.getBuyerCookie() == null || request.getBuyerCookie().trim().isEmpty()) {
            throw new IllegalArgumentException("BuyerCookie is required");
        }
        
        if (request.getBrowserFormPostUrl() == null || request.getBrowserFormPostUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("BrowserFormPost.URL is required");
        }
        
        // SSRF防护：校验returnUrl
        validateReturnUrl(request.getBrowserFormPostUrl());
        
        if (request.getSenderIdentity() == null || request.getSenderIdentity().trim().isEmpty()) {
            throw new IllegalArgumentException("Sender.Identity is required");
        }
        
        if (request.getSenderSharedSecret() == null || request.getSenderSharedSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("Sender.SharedSecret is required");
        }
        
        // From（Buyer）信息必须存在（用于后续Return）
        if (request.getFromIdentity() == null || request.getFromIdentity().trim().isEmpty()) {
            throw new IllegalArgumentException("From.Identity is required (Buyer identity for return)");
        }
        
        if (request.getFromDomain() == null || request.getFromDomain().trim().isEmpty()) {
            throw new IllegalArgumentException("From.Domain is required (Buyer domain for return)");
        }
        
        // 可选但建议校验的字段
        if (request.getPayloadId() == null || request.getPayloadId().trim().isEmpty()) {
            log.warn("PayloadID is missing in request (may affect troubleshooting)");
        }
        
        if (request.getTimestamp() != null && !request.getTimestamp().trim().isEmpty()) {
            try {
                java.time.OffsetDateTime.parse(request.getTimestamp());
            } catch (Exception e) {
                log.warn("Invalid timestamp format in request: {}", request.getTimestamp());
            }
        }
        
        // Operation校验（只允许create/edit）
        String operation = request.getOperation();
        if (operation != null && !operation.trim().isEmpty() && 
            !"create".equals(operation) && !"edit".equals(operation)) {
            log.warn("Unsupported operation: {} (expected 'create' or 'edit')", operation);
        }
    }
    
    /**
     * 校验returnUrl（SSRF防护）
     * 
     * @param returnUrl 返回URL
     * @throws IllegalArgumentException 如果URL不安全
     */
    private void validateReturnUrl(String returnUrl) {
        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Return URL is empty");
        }
        
        try {
            URL url = new URL(returnUrl);
            String protocol = url.getProtocol();
            String host = url.getHost();
            
            // 1. 必须是HTTPS（生产环境）
            // 开发环境可以允许HTTP，但生产环境必须HTTPS
            if (!"https".equalsIgnoreCase(protocol) && !"http".equalsIgnoreCase(protocol)) {
                throw new IllegalArgumentException("Return URL must use HTTP or HTTPS protocol");
            }
            
            // 2. 禁止内网地址
            if (isPrivateNetworkAddress(host)) {
                throw new IllegalArgumentException("Return URL cannot point to private network address: " + host);
            }
            
            // 3. 必须在允许的host列表中
            if (allowedReturnHosts.isEmpty()) {
                log.warn("No allowed return hosts configured. Allowing all hosts (not recommended for production)");
            } else {
                if (!allowedReturnHosts.contains(host)) {
                    throw new IllegalArgumentException("Return URL host is not in allowed list: " + host);
                }
            }
            
            log.debug("Return URL validation passed: {}", sanitizeUrlForLogging(returnUrl));
            
        } catch (IllegalArgumentException e) {
            throw e;  // 重新抛出验证异常
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid return URL format: " + e.getMessage());
        }
    }
    
    /**
     * 检查host是否为内网地址
     */
    private boolean isPrivateNetworkAddress(String host) {
        if (host == null || host.isEmpty()) {
            return true;
        }
        
        // 检查是否为localhost
        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || 
            "::1".equals(host) || "0.0.0.0".equals(host)) {
            return true;
        }
        
        try {
            InetAddress addr = InetAddress.getByName(host);
            byte[] ip = addr.getAddress();
            
            // 检查是否为内网IP
            // 10.0.0.0/8
            if (ip.length == 4 && ip[0] == 10) {
                return true;
            }
            // 172.16.0.0/12
            if (ip.length == 4 && ip[0] == (byte)172 && ip[1] >= 16 && ip[1] <= 31) {
                return true;
            }
            // 192.168.0.0/16
            if (ip.length == 4 && ip[0] == (byte)192 && ip[1] == (byte)168) {
                return true;
            }
            // 169.254.0.0/16 (Link-local)
            if (ip.length == 4 && ip[0] == (byte)169 && ip[1] == (byte)254) {
                return true;
            }
            
        } catch (Exception e) {
            // 如果无法解析，可能是域名，继续检查
            log.debug("Could not resolve host to IP: {}", host);
        }
        
        return false;
    }
    
    /**
     * 根据email查找用户
     */
    private User findUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return userRepository.findByEmail(email.trim()).orElse(null);
    }
    
    /**
     * 生成cXML格式的响应
     * 关联request的payloadID用于日志追踪
     */
    private String generateCxmlResponse(CxmlSetupRequest request, String redirectUrl) {
        // 生成新的payloadID
        String requestPayloadId = request.getPayloadId();
        String responsePayloadId = "punchout-response-" + System.currentTimeMillis() + "@" + getHostname();
        String timestamp = java.time.OffsetDateTime.now().toString();
        
        // 记录关联信息到日志（不混入responsePayloadID，避免特殊字符问题）
        log.info("Generating cXML response (requestPayloadID: {}, responsePayloadID: {})", 
            requestPayloadId, responsePayloadId);
        
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd\">\n" +
               "<cXML xml:lang=\"en-US\" payloadID=\"" + escapeXml(responsePayloadId) + "\" timestamp=\"" + escapeXml(timestamp) + "\">\n" +
               "  <Response>\n" +
               "    <Status code=\"200\" text=\"OK\"/>\n" +
               "    <PunchOutSetupResponse>\n" +
               "      <StartPage>\n" +
               "        <URL>" + escapeXml(redirectUrl) + "</URL>\n" +
               "      </StartPage>\n" +
               "    </PunchOutSetupResponse>\n" +
               "  </Response>\n" +
               "</cXML>";
    }
    
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * 处理punchout setup请求（JSON格式，保持向后兼容）
     * 
     * @param request punchout setup请求
     * @return punchout setup响应
     */
    public PunchoutSetupResponse setupPunchout(PunchoutSetupRequest request) {
        log.info("Processing punchout setup request for buyerCookie: {}", request.getBuyerCookie());

        // 根据buyerCookie或buyerId查找用户
        // 这里假设buyerId是用户的email，或者buyerCookie可以用来识别用户
        User user = findUserByBuyerInfo(request.getBuyerCookie(), request.getBuyerId());
        
        if (user == null) {
            throw new RuntimeException("User not found for buyerCookie: " + request.getBuyerCookie());
        }

        // 生成会话token
        String sessionToken = UUID.randomUUID().toString();
        
        // 创建会话（30分钟过期）
        Instant expiresAt = Instant.now().plusSeconds(1800);  // 30分钟 = 1800秒
        // 注意：JSON格式的setup请求没有完整的cXML Header信息
        // 如果后续需要Return回传，这些信息是必需的
        // 这里设置为null，在return时会检查并拒绝
        PunchoutSession session = new PunchoutSession(
            user.getId(),
            request.getBuyerCookie(),
            request.getBuyerId(),
            request.getBuyerName(),
            request.getOrganizationId(),
            expiresAt,
            request.getReturnUrl(),
            // Buyer信息（JSON格式缺失，设置为null）
            null,  // buyerIdentity
            null,  // buyerDomain
            null   // senderIdentity
        );
        
        sessionStore.put(sessionToken, session);
        
        // 构建重定向URL（只包含sessionToken，不包含JWT token，避免安全风险）
        // 前端需要使用sessionToken调用后端接口换取登录态
        String redirectUrl = String.format("%s/punchout/shopping?sessionToken=%s",
            frontendUrl, sessionToken);

        log.info("Punchout setup successful, redirectUrl: {}", redirectUrl);

        return new PunchoutSetupResponse(
            sessionToken,
            redirectUrl,
            1800L // 30分钟 = 1800秒
        );
    }

    /**
     * 处理punchout return请求（标准PunchOut Return流程）
     * 
     * 标准流程：
     * 1. 验证会话token
     * 2. 读取session.returnUrl (BrowserFormPost.URL)
     * 3. 生成PunchOutOrderMessage cXML
     * 4. POST cXML到returnUrl
     * 5. 返回结果给前端
     * 
     * @param request punchout return请求
     * @return punchout return响应
     */
    public PunchoutReturnResponse returnPunchout(PunchoutReturnRequest request) {
        log.info("Processing punchout return request with sessionToken: {}", request.getSessionToken());

        // 验证会话token
        PunchoutSession session = sessionStore.get(request.getSessionToken());
        
        if (session == null) {
            log.error("Invalid session token: {}", request.getSessionToken());
            return new PunchoutReturnResponse(null, null, null, false, "Invalid session token", null);
        }

        if (session.isExpired()) {
            log.error("Session expired: {}", request.getSessionToken());
            sessionStore.remove(request.getSessionToken());
            return new PunchoutReturnResponse(null, null, null, false, "Session expired", null);
        }

        // 检查returnUrl
        if (session.returnUrl == null || session.returnUrl.trim().isEmpty()) {
            log.error("Return URL is missing in session: {}", request.getSessionToken());
            return new PunchoutReturnResponse(null, null, null, false, "Return URL is missing", null);
        }

        // 验证购物车项
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.warn("Empty cart in punchout return request");
            return new PunchoutReturnResponse(null, null, null, false, "Cart is empty", null);
        }

        // 检查Header信息是否完整（JSON setup可能缺失）
        if (session.buyerIdentity == null || session.buyerIdentity.trim().isEmpty()) {
            log.error("Buyer identity is missing in session (likely from JSON setup). Cannot return to buyer system.");
            return new PunchoutReturnResponse(null, null, null, false, 
                "Buyer identity is missing. This session was created via JSON setup and cannot be used for cXML return.", null);
        }
        
        if (session.buyerDomain == null || session.buyerDomain.trim().isEmpty()) {
            log.error("Buyer domain is missing in session. Cannot return to buyer system.");
            return new PunchoutReturnResponse(null, null, null, false, 
                "Buyer domain is missing. Cannot build valid PunchOutOrderMessage.", null);
        }
        
        // 检查重试次数（避免无限重试）
        int currentAttempts = session.getReturnAttempts();
        if (currentAttempts >= 3) {
            log.error("Return attempts exceeded limit ({}). Clearing session.", currentAttempts);
            sessionStore.remove(request.getSessionToken());
            return new PunchoutReturnResponse(null, null, null, false, 
                "Return attempts exceeded (3). Please restart punchout session.", 0);
        }

        // 单次提交锁：防止并发重复提交
        if (!session.returning.compareAndSet(false, true)) {
            log.warn("Return is already in progress for sessionToken: {}", request.getSessionToken());
            int remainingAttempts = Math.max(0, 3 - session.getReturnAttempts());
            return new PunchoutReturnResponse(null, null, null, false, 
                "Return is already in progress. Please wait.", remainingAttempts);
        }

        try {
            // 增加重试计数（线程安全）
            int newAttempts = session.incrementReturnAttempts();
            int remainingAttempts = Math.max(0, 3 - newAttempts);  // 防止负数
            
            // 步骤1: 生成PunchOutOrderMessage cXML
            // From = Supplier (从配置读取), To = Buyer (从session读取)
            log.info("Building PunchOutOrderMessage cXML for buyerCookie: {} (attempt: {}, remaining: {})", 
                session.buyerCookie, newAttempts, remainingAttempts);
            String cxmlContent = orderMessageBuilder.buildPunchOutOrderMessage(
                session.buyerCookie,
                supplierIdentity,      // From: Supplier Identity (从配置)
                supplierDomain,        // From: Supplier Domain (从配置)
                session.buyerIdentity, // To: Buyer Identity (从SetupRequest的From读取)
                session.buyerDomain,   // To: Buyer Domain (从SetupRequest的From读取)
                supplierIdentity,      // Sender: Supplier Identity (从配置)
                request.getItems()
            );
            
            log.debug("Generated PunchOutOrderMessage cXML (length: {})", cxmlContent.length());
            
            // 步骤2: POST cXML到采购系统的returnUrl
            String sanitizedReturnUrl = sanitizeUrlForLogging(session.returnUrl);
            log.info("Posting PunchOutOrderMessage to buyer system: {} (attempt: {}, remaining: {})", 
                sanitizedReturnUrl, newAttempts, remainingAttempts);
            PunchoutReturnClient.ReturnResult returnResult = returnClient.postPunchOutOrderMessage(
                session.returnUrl, cxmlContent);
            
            // 步骤3: 检查业务状态（解析cXML响应）
            // 注意：测试模式的处理已经在 PunchoutReturnClient 中完成
            if (!returnResult.isBusinessSuccess()) {
                log.error("Buyer system returned business error: HTTP={}, code={}, text={}, message={} (attempt: {}, remaining: {})", 
                    returnResult.getHttpStatusCode(), returnResult.getStatusCode(), returnResult.getStatusText(), 
                    returnResult.getErrorMessage(), newAttempts, remainingAttempts);
                // 失败时释放锁，允许重试（但会检查重试次数限制）
                session.returning.set(false);
                
                // 构建更详细的错误消息
                String errorMsg;
                if (returnResult.getErrorMessage() != null && !returnResult.getErrorMessage().isEmpty()) {
                    // 使用解析到的错误消息
                    errorMsg = returnResult.getErrorMessage();
                } else if (returnResult.getStatusCode() != null && returnResult.getStatusText() != null) {
                    // 使用Status code和text
                    errorMsg = String.format("Buyer system returned error: %s - %s", 
                        returnResult.getStatusCode(), returnResult.getStatusText());
                } else if (returnResult.getStatusCode() != null) {
                    // 只有code
                    errorMsg = String.format("Buyer system returned error code: %s", returnResult.getStatusCode());
                } else {
                    // 都没有，使用HTTP状态码
                    errorMsg = String.format("Buyer system returned HTTP %s. Response may not be in cXML format or parsing failed.", 
                        returnResult.getHttpStatusCode());
                }
                
                return new PunchoutReturnResponse(
                    null, null, null, false,
                    errorMsg,
                    remainingAttempts  // 返回剩余重试次数，前端可以提示用户
                );
            }
            
            log.info("Successfully posted PunchOutOrderMessage to buyer system. HTTP status: {}, Business status: {} - {}", 
                returnResult.getHttpStatusCode(), returnResult.getStatusCode(), returnResult.getStatusText());
            
            // 步骤4: 清理会话（只有业务成功才清理，锁会随session一起释放）
            sessionStore.remove(request.getSessionToken());
            
            // 步骤5: 返回成功响应
            // 注意：这里不创建内部订单，因为购物车已经返回给采购系统
            // 采购系统会处理后续的PR/PO流程
            return new PunchoutReturnResponse(
                null,  // 不返回订单ID（因为订单在采购系统中创建）
                null,  // 不返回订单号
                "returned",  // 状态：已返回给采购系统
                true,
                "Cart successfully returned to buyer system",
                null  // 成功时不需要剩余重试次数
            );

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            // 释放锁
            session.returning.set(false);
            return new PunchoutReturnResponse(null, null, null, false, "Invalid request: " + e.getMessage(), null);
        } catch (Exception e) {
            log.error("Error processing punchout return: ", e);
            // 获取当前剩余重试次数
            PunchoutSession failedSession = sessionStore.get(request.getSessionToken());
            int remainingAttempts = failedSession != null ? 
                Math.max(0, 3 - failedSession.getReturnAttempts()) : 0;  // 防止负数
            // 释放锁，允许重试
            if (failedSession != null) {
                failedSession.returning.set(false);
            }
            return new PunchoutReturnResponse(null, null, null, false, 
                "Error returning cart to buyer system: " + e.getMessage(), remainingAttempts);
        }
    }

    /**
     * 根据buyer信息查找用户
     */
    private User findUserByBuyerInfo(String buyerCookie, String buyerId) {
        // 策略1: 如果buyerId是email，直接查找
        if (buyerId != null && !buyerId.isEmpty()) {
            Optional<User> userByEmail = userRepository.findByEmail(buyerId);
            if (userByEmail.isPresent()) {
                return userByEmail.get();
            }
        }

        // 策略2: 如果buyerCookie包含email信息，尝试解析
        // 这里可以根据实际业务需求调整查找逻辑
        
        // 策略3: 如果都没有，返回第一个用户（仅用于开发测试）
        // 生产环境应该根据实际业务逻辑实现
        List<User> allUsers = userRepository.findAll();
        if (!allUsers.isEmpty()) {
            log.warn("Using first available user for punchout (buyerCookie: {}, buyerId: {})", 
                buyerCookie, buyerId);
            return allUsers.get(0);
        }

        return null;
    }


    /**
     * 验证会话token（用于前端验证）
     */
    /**
     * 验证会话token
     * 
     * @param sessionToken 会话token
     * @return 验证结果对象，包含是否有效和详细信息
     */
    public SessionValidationResult validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return new SessionValidationResult(false, "Session token is empty");
        }
        
        PunchoutSession session = sessionStore.get(sessionToken);
        
        if (session == null) {
            log.debug("Session not found for token: {}", sessionToken);
            return new SessionValidationResult(false, "Session not found. Please run Setup request first.");
        }
        
        if (session.isExpired()) {
            log.debug("Session expired for token: {}, expired at: {}", sessionToken, session.expiresAt);
            return new SessionValidationResult(false, 
                String.format("Session expired at %s. Please run Setup request again.", session.expiresAt));
        }
        
        return new SessionValidationResult(true, "Session is valid");
    }
    
    /**
     * 验证会话token（向后兼容，返回boolean）
     * 
     * @param sessionToken 会话token
     * @return 是否有效
     */
    public boolean validateSessionBoolean(String sessionToken) {
        return validateSession(sessionToken).isValid();
    }
    
    /**
     * 会话验证结果
     */
    public static class SessionValidationResult {
        private final boolean valid;
        private final String message;
        
        public SessionValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * URL脱敏：只显示host和path，隐藏query参数
     */
    private String sanitizeUrlForLogging(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        try {
            URL urlObj = new URL(url);
            // 只返回 protocol + host + path，不包含query和fragment
            return urlObj.getProtocol() + "://" + urlObj.getHost() + 
                   (urlObj.getPort() != -1 ? ":" + urlObj.getPort() : "") + 
                   (urlObj.getPath() != null ? urlObj.getPath() : "");
        } catch (Exception e) {
            // 如果解析失败，截断显示前100个字符
            return url.length() > 100 ? url.substring(0, 100) + "..." : url;
        }
    }
    
    /**
     * 定时清理过期的session（每5分钟执行一次）
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 300000)  // 5分钟 = 300000毫秒
    public void cleanupExpiredSessions() {
        int removedCount = 0;
        
        Iterator<Map.Entry<String, PunchoutSession>> iterator = sessionStore.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PunchoutSession> entry = iterator.next();
            PunchoutSession session = entry.getValue();
            if (session.isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            log.info("Cleaned up {} expired punchout sessions. Remaining sessions: {}", 
                removedCount, sessionStore.size());
        }
    }
}

