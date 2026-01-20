package com.ecosystem.dto.punchout;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Punchout Setup Request - 来自采购系统的punchout设置请求
 */
@Data
public class PunchoutSetupRequest {
    
    /**
     * 采购系统的用户标识（buyer cookie）
     */
    @NotBlank(message = "Buyer cookie is required")
    private String buyerCookie;
    
    /**
     * 采购系统的用户ID
     */
    private String buyerId;
    
    /**
     * 采购系统的用户名
     */
    private String buyerName;
    
    /**
     * 采购系统的组织ID
     */
    private String organizationId;
    
    /**
     * 返回URL（采购系统接收购物车数据的URL）
     */
    private String returnUrl;
    
    /**
     * 操作类型（通常为"create"）
     */
    private String operation = "create";
}

