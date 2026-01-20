package com.ecosystem.dto.punchout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Punchout Setup Response - 返回给采购系统的响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PunchoutSetupResponse {
    
    /**
     * 会话token，用于后续验证
     */
    private String sessionToken;
    
    /**
     * 重定向URL，采购系统应该重定向用户到这个URL
     */
    private String redirectUrl;
    
    /**
     * 会话过期时间（秒）
     */
    private Long expiresIn;
}

