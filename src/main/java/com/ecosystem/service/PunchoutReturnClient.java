package com.ecosystem.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Punchout Return Client
 * 负责将 PunchOutOrderMessage cXML POST 到采购系统的 BrowserFormPost.URL
 * 并解析响应，检查业务状态
 */
@Component
@Slf4j
public class PunchoutReturnClient {

    private final RestTemplate restTemplate;
    
    @Value("${punchout.test-mode-allow-empty-response:false}")
    private boolean testModeAllowEmptyResponse;

    public PunchoutReturnClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Return结果
     */
    @Data
    @AllArgsConstructor
    public static class ReturnResult {
        private HttpStatusCode httpStatusCode;
        private String responseBody;
        private String statusCode;  // cXML Status code
        private String statusText;  // cXML Status text
        private String errorMessage;
        
        /**
         * 业务是否成功（HTTP 200 且 cXML Status code = 200）
         * 在测试模式下，如果 HTTP 200 但响应体为空，也视为成功
         */
        public boolean isBusinessSuccess(boolean testModeAllowEmptyResponse) {
            // 标准成功：HTTP 200 且 cXML Status code = 200
            if (httpStatusCode.is2xxSuccessful() && 
                statusCode != null && "200".equals(statusCode)) {
                return true;
            }
            
            // 测试模式：HTTP 200 但响应体为空时也视为成功
            if (testModeAllowEmptyResponse && 
                httpStatusCode.is2xxSuccessful() && 
                (responseBody == null || responseBody.trim().isEmpty())) {
                return true;
            }
            
            return false;
        }
        
        /**
         * 业务是否成功（使用默认配置，不启用测试模式）
         */
        public boolean isBusinessSuccess() {
            return isBusinessSuccess(false);
        }
    }

    /**
     * 将 PunchOutOrderMessage cXML POST 到采购系统的返回URL
     * 并解析响应，检查业务状态
     * 
     * @param returnUrl 采购系统的返回URL（BrowserFormPost.URL）
     * @param cxmlContent PunchOutOrderMessage cXML内容
     * @return ReturnResult 包含HTTP状态和业务状态
     * @throws Exception 如果POST失败
     */
    public ReturnResult postPunchOutOrderMessage(String returnUrl, String cxmlContent) throws Exception {
        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Return URL is required");
        }
        
        if (cxmlContent == null || cxmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("cXML content is required");
        }
        
        // URL脱敏：只显示host和path，隐藏query参数
        String sanitizedUrl = sanitizeUrlForLogging(returnUrl);
        log.info("Posting PunchOutOrderMessage to: {}", sanitizedUrl);
        log.debug("cXML content (first 500 chars): {}", 
            cxmlContent.length() > 500 ? cxmlContent.substring(0, 500) + "..." : cxmlContent);
        
        try {
            // 设置HTTP Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("User-Agent", "Ecosystem Backend/1.0");
            
            // 创建HTTP Entity
            HttpEntity<String> request = new HttpEntity<>(cxmlContent, headers);
            
            // POST请求
            ResponseEntity<String> response = restTemplate.exchange(
                returnUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            HttpStatusCode httpStatusCode = response.getStatusCode();
            String responseBody = response.getBody();
            
            log.info("PunchOutOrderMessage POST response: HTTP status={}, bodyLength={}, headers={}", 
                httpStatusCode, 
                responseBody != null ? responseBody.length() : 0,
                response.getHeaders());
            
            // 记录响应内容（用于调试）
            if (responseBody != null && responseBody.length() > 0) {
                log.debug("Response body preview (first 500 chars): {}", 
                    responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
            }
            
            // 解析cXML响应，提取Status
            String statusCode = null;
            String statusText = null;
            String errorMessage = null;
            
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                if (responseBody.trim().startsWith("<?xml") || responseBody.trim().startsWith("<")) {
                    try {
                        // 解析cXML响应
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        // 安全配置：防止XXE攻击
                        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
                        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                        factory.setXIncludeAware(false);
                        factory.setExpandEntityReferences(false);
                        
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document document = builder.parse(
                            new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)));
                        
                        // 查找Status元素
                        NodeList statusList = document.getElementsByTagName("Status");
                        if (statusList.getLength() > 0) {
                            Element status = (Element) statusList.item(0);
                            statusCode = status.getAttribute("code");
                            statusText = status.getAttribute("text");
                            
                            log.info("Parsed cXML Status: code={}, text={}", statusCode, statusText);
                            
                            // 如果状态不是200，提取错误信息
                            if (!"200".equals(statusCode)) {
                                // 尝试提取Message元素
                                NodeList messageList = document.getElementsByTagName("Message");
                                if (messageList.getLength() > 0) {
                                    errorMessage = messageList.item(0).getTextContent();
                                } else {
                                    errorMessage = statusText != null && !statusText.isEmpty() ? statusText : "Unknown error";
                                }
                            }
                        } else {
                            // XML格式正确但没有Status元素
                            log.warn("cXML response does not contain Status element");
                            errorMessage = "Buyer system response does not contain Status element";
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse cXML response: {}", e.getMessage());
                        // 如果解析失败，记录响应内容的前200个字符用于调试
                        String responsePreview = responseBody.length() > 200 ? 
                            responseBody.substring(0, 200) + "..." : responseBody;
                        errorMessage = String.format("Failed to parse buyer system response: %s. Response preview: %s", 
                            e.getMessage(), responsePreview);
                    }
                } else {
                    // 响应不是XML格式
                    log.warn("Buyer system response is not XML format. Content-Type may be incorrect.");
                    String responsePreview = responseBody.length() > 200 ? 
                        responseBody.substring(0, 200) + "..." : responseBody;
                    errorMessage = String.format("Buyer system returned non-XML response. Preview: %s", responsePreview);
                }
            } else {
                // 响应体为空
                log.warn("Buyer system returned empty response body. HTTP status: {}, URL: {}", 
                    httpStatusCode, sanitizedUrl);
                if (httpStatusCode.is2xxSuccessful()) {
                    // HTTP 200 但响应体为空，可能是某些系统的正常行为
                    errorMessage = String.format(
                        "Buyer system returned HTTP %d with empty response body. This may be normal for some systems. " +
                        "If this is a test environment, the returnUrl may not be accessible: %s", 
                        httpStatusCode.value(), sanitizedUrl);
                } else {
                    errorMessage = String.format(
                        "Buyer system returned HTTP %d with empty response body. URL: %s", 
                        httpStatusCode.value(), sanitizedUrl);
                }
            }
            
            ReturnResult result = new ReturnResult(
                httpStatusCode,
                responseBody,
                statusCode,
                statusText,
                errorMessage
            );
            
            // 检查业务成功（考虑测试模式）
            boolean isSuccess = result.isBusinessSuccess(testModeAllowEmptyResponse);
            
            if (isSuccess) {
                if (testModeAllowEmptyResponse && (responseBody == null || responseBody.trim().isEmpty())) {
                    log.info("Successfully posted PunchOutOrderMessage to buyer system (test mode: HTTP {} with empty response treated as success)", 
                        httpStatusCode);
                } else {
                    log.info("Successfully posted PunchOutOrderMessage to buyer system (business success)");
                }
            } else {
                log.warn("Buyer system returned non-success status: HTTP={}, Business={} - {}", 
                    httpStatusCode, statusCode, statusText);
            }
            
            return result;
            
        } catch (HttpClientErrorException e) {
            log.error("Client error posting to buyer system ({}): HTTP {}, Response: {}", 
                sanitizedUrl, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                String.format("Buyer system returned client error (HTTP %d): %s. URL: %s", 
                    e.getStatusCode().value(), e.getMessage(), sanitizedUrl), e);
        } catch (HttpServerErrorException e) {
            log.error("Server error from buyer system ({}): HTTP {}, Response: {}", 
                sanitizedUrl, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                String.format("Buyer system returned server error (HTTP %d): %s. URL: %s", 
                    e.getStatusCode().value(), e.getMessage(), sanitizedUrl), e);
        } catch (RestClientException e) {
            log.error("Network error posting to buyer system ({}): {}", sanitizedUrl, e.getMessage(), e);
            throw new RuntimeException(
                String.format("Network error posting to buyer system: %s. URL: %s. " +
                    "This may indicate the returnUrl is not accessible or the network is unreachable.", 
                    e.getMessage(), sanitizedUrl), e);
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
            java.net.URL urlObj = new java.net.URL(url);
            // 只返回 protocol + host + path，不包含query和fragment
            return urlObj.getProtocol() + "://" + urlObj.getHost() + 
                   (urlObj.getPort() != -1 ? ":" + urlObj.getPort() : "") + 
                   (urlObj.getPath() != null ? urlObj.getPath() : "");
        } catch (Exception e) {
            // 如果解析失败，截断显示前100个字符
            return url.length() > 100 ? url.substring(0, 100) + "..." : url;
        }
    }
}
