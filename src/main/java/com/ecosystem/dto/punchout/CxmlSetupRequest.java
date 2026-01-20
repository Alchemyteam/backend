package com.ecosystem.dto.punchout;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * cXML格式的Punchout Setup Request
 * 根据设计原则：协议骨架严格，业务字段宽松
 */
@Data
public class CxmlSetupRequest {
    
    // ==================== 协议骨架（严格校验） ====================
    
    /**
     * BuyerCookie - 必须字段
     */
    private String buyerCookie;
    
    /**
     * BrowserFormPost.URL - 必须字段（返回URL）
     */
    private String browserFormPostUrl;
    
    /**
     * Sender.Identity - 必须字段（用于认证）
     */
    private String senderIdentity;
    
    /**
     * Sender.SharedSecret - 必须字段（用于认证）
     */
    private String senderSharedSecret;
    
    /**
     * Sender.Domain - 可选但建议校验
     */
    private String senderDomain;
    
    /**
     * Operation - 操作类型（通常为"create"）
     */
    private String operation = "create";
    
    // ==================== 业务字段（宽松处理） ====================
    
    /**
     * Extrinsic字段 - 键值对形式存储
     * 例如：FirstName, LastName, UniqueName, UserEmail等
     */
    private Map<String, String> extrinsic = new HashMap<>();
    
    /**
     * Contact信息 - 可选
     */
    private ContactInfo contact;
    
    /**
     * ShipTo信息 - 可选
     */
    private ShipToInfo shipTo;
    
    /**
     * From.Identity - 可选
     */
    private String fromIdentity;
    
    /**
     * From.Domain - 可选
     */
    private String fromDomain;
    
    /**
     * To.Identity - 可选
     */
    private String toIdentity;
    
    /**
     * To.Domain - 可选
     */
    private String toDomain;
    
    /**
     * UserAgent - 可选
     */
    private String userAgent;
    
    /**
     * PayloadID - 可选
     */
    private String payloadId;
    
    /**
     * Timestamp - 可选
     */
    private String timestamp;
    
    /**
     * Contact信息内部类
     */
    @Data
    public static class ContactInfo {
        private String name;
        private String email;
        private String role;
    }
    
    /**
     * ShipTo信息内部类
     */
    @Data
    public static class ShipToInfo {
        private String addressId;
        private String name;
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }
}

