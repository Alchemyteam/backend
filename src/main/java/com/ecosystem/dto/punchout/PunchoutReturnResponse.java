package com.ecosystem.dto.punchout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Punchout Return Response - 返回给购物网站的响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PunchoutReturnResponse {
    
    /**
     * 订单ID
     */
    private String orderId;
    
    /**
     * 订单号
     */
    private String orderNumber;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 错误消息（如果有）
     */
    private String message;
    
    /**
     * 剩余重试次数（失败时返回）
     */
    private Integer remainingAttempts;
}

