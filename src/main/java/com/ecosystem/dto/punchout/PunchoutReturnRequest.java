package com.ecosystem.dto.punchout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Punchout Return Request - 从购物网站返回的购物车数据
 */
@Data
public class PunchoutReturnRequest {
    
    /**
     * 会话token，用于验证请求
     */
    @NotBlank(message = "Session token is required")
    private String sessionToken;
    
    /**
     * 购物车项列表
     */
    @NotEmpty(message = "Cart items are required")
    @Valid  // 验证列表中的每个元素
    private List<PunchoutCartItem> items;
    
    /**
     * 采购系统的订单号（可选）
     */
    private String buyerOrderNumber;
    
    /**
     * 备注信息（可选）
     */
    private String notes;
}

