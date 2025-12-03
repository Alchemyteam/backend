package com.ecosystem.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String response; // LLM的文本回复
    private String conversationId; // 对话ID
    private TableData tableData; // 表格数据（如果有）
    private ActionData actionData; // 操作数据（如创建采购需求等）
}

