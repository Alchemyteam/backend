package com.ecosystem.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionData {
    private String actionType; // 操作类型：CREATE_REQUISITION, SEARCH_PRODUCTS, etc.
    private Map<String, Object> parameters; // 操作参数
    private String message; // 操作结果消息
}


