package com.ecosystem.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message is required")
    private String message;
    
    private String conversationId; // 可选，用于多轮对话
}


