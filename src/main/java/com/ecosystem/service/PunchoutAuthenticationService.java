package com.ecosystem.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Punchout认证服务
 * 验证Sender的Identity和SharedSecret
 */
@Service
@Slf4j
public class PunchoutAuthenticationService {

    @Value("${punchout.allowed-senders:}")
    private String allowedSendersConfig;

    // 存储允许的发送者配置（Identity -> SharedSecret）
    // 使用ConcurrentHashMap保证线程安全（支持动态添加）
    // 生产环境应该从数据库或配置文件加载，且SharedSecret应该加密存储
    private final Map<String, String> allowedSenders = new ConcurrentHashMap<>();

    /**
     * 初始化允许的发送者列表
     * 使用@PostConstruct确保@Value注入完成后再执行
     * 
     * 注意：生产环境建议：
     * 1. SharedSecret不写死在代码中
     * 2. 使用环境变量或配置中心
     * 3. 如果可能，使用加密/Hash存储
     */
    @PostConstruct
    public void init() {
        initializeAllowedSenders();
    }

    /**
     * 初始化允许的发送者列表
     * 
     * 注意：生产环境安全建议：
     * - SharedSecret不应写死在代码中
     * - 应使用环境变量或配置中心
     * - 如果可能，使用加密/Hash存储SharedSecret
     */
    private void initializeAllowedSenders() {
        // 默认配置（示例，仅用于开发/测试环境）
        // 生产环境应该移除硬编码，完全依赖配置文件或数据库
        allowedSenders.put("AIR_LIQUIDE", "s3cr3tk3y!");
        
        // 如果配置文件中指定了，则使用配置文件的值（覆盖默认值）
        // 格式：identity1:secret1,identity2:secret2
        if (allowedSendersConfig != null && !allowedSendersConfig.trim().isEmpty()) {
            String[] pairs = allowedSendersConfig.split(",");
            for (String pair : pairs) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    String identity = parts[0].trim();
                    String secret = parts[1].trim();
                    if (!identity.isEmpty() && !secret.isEmpty()) {
                        allowedSenders.put(identity, secret);
                        log.debug("Loaded sender from config: {}", identity);
                    }
                } else {
                    log.warn("Invalid sender config format (expected 'identity:secret'): {}", pair);
                }
            }
        }
        
        log.info("Initialized allowed senders: {} (total: {})", allowedSenders.keySet(), allowedSenders.size());
    }

    /**
     * 验证发送者身份
     * 
     * @param identity 发送者标识
     * @param sharedSecret 共享密钥
     * @return 验证是否通过
     */
    public boolean authenticate(String identity, String sharedSecret) {
        if (identity == null || identity.trim().isEmpty()) {
            log.warn("Sender identity is empty");
            return false;
        }
        
        if (sharedSecret == null || sharedSecret.trim().isEmpty()) {
            log.warn("Shared secret is empty for identity: {}", identity);
            return false;
        }
        
        String expectedSecret = allowedSenders.get(identity);
        if (expectedSecret == null) {
            log.warn("Unknown sender identity: {}", identity);
            return false;
        }
        
        boolean authenticated = expectedSecret.equals(sharedSecret);
        if (authenticated) {
            log.info("Successfully authenticated sender: {}", identity);
        } else {
            log.warn("Authentication failed for sender: {} (invalid shared secret)", identity);
        }
        
        return authenticated;
    }

    /**
     * 添加允许的发送者（用于动态配置）
     */
    public void addAllowedSender(String identity, String sharedSecret) {
        allowedSenders.put(identity, sharedSecret);
        log.info("Added allowed sender: {}", identity);
    }
}

