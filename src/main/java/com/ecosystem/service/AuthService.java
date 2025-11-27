package com.ecosystem.service;

import com.ecosystem.dto.AuthResponse;
import com.ecosystem.dto.LoginRequest;
import com.ecosystem.dto.RegisterRequest;
import com.ecosystem.dto.UserResponse;
import com.ecosystem.entity.User;
import com.ecosystem.exception.EmailAlreadyExistsException;
import com.ecosystem.exception.InvalidCredentialsException;
import com.ecosystem.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查邮箱是否已存在
        if (userService.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // 创建用户
        User user = userService.createUser(
                request.getEmail(),
                request.getPassword(),
                request.getName()
        );

        // 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 返回响应
        UserResponse userResponse = userService.toUserResponse(user);
        return new AuthResponse(token, userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        // 查找用户
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        // 验证密码
        if (!userService.validatePassword(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 返回响应
        UserResponse userResponse = userService.toUserResponse(user);
        return new AuthResponse(token, userResponse);
    }

    public UserResponse getCurrentUser(String userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userService.toUserResponse(user);
    }

    public boolean verifyToken(String token) {
        return jwtUtil.validateToken(token);
    }
}

