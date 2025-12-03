package com.ecosystem.controller;

import com.ecosystem.dto.chat.ChatRequest;
import com.ecosystem.dto.chat.ChatResponse;
import com.ecosystem.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @Valid @RequestBody ChatRequest request,
            Authentication authentication) {
        // userId 可以用于后续的对话历史记录等功能
        String conversationId = request.getConversationId();
        
        ChatResponse response = chatService.processMessage(request.getMessage(), conversationId);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "chat"));
    }
}

