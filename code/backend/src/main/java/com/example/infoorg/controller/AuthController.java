package com.example.infoorg.controller;

import com.example.infoorg.mapper.UserMapper;
import com.example.infoorg.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        Map<String, Object> user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        String storedHash = (String) user.get("password_hash");
        if (!passwordEncoder.matches(password, storedHash) && !password.equals(storedHash)) {
            throw new RuntimeException("用户名或密码错误");
        }

        String userId = (String) user.get("id");
        String token = jwtUtil.generateToken(userId, username);
        return Map.of(
                "token", token,
                "userId", userId,
                "username", username
        );
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.isValid(token)) {
            throw new RuntimeException("Token 已过期");
        }
        String userId = jwtUtil.getUserId(token);
        Map<String, Object> user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }
}
