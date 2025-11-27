package com.ecosystem.controller;

import com.ecosystem.dto.AuthResponse;
import com.ecosystem.dto.ErrorResponse;
import com.ecosystem.dto.LoginRequest;
import com.ecosystem.dto.RegisterRequest;
import com.ecosystem.dto.UserResponse;
import com.ecosystem.dto.VerifyResponse;
import com.ecosystem.service.AuthService;
import com.ecosystem.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String userId = authentication.getName();
            UserResponse user = authService.getCurrentUser(userId);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Unauthorized",
                    null,
                    "Invalid or expired token"
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            VerifyResponse response = new VerifyResponse(false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String token = authHeader.substring(7);
        boolean isValid = authService.verifyToken(token);

        if (isValid) {
            VerifyResponse response = new VerifyResponse(true);
            return ResponseEntity.ok(response);
        } else {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Token is invalid or expired",
                    null,
                    null
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}

