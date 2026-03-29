package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final com.buildmat.security.JwtUtil jwt;

    public ResponseEntity<?> login(String username, String password) {
        Optional<UserEntity> opt = userRepo.findByUsernameAndActiveTrue(username.trim().toLowerCase());
        if (opt.isEmpty() || !encoder.matches(password, opt.get().getPasswordHash()))
            return ResponseEntity.status(401).body(Map.of("error","Invalid username or password"));
        UserEntity u = opt.get();
        String token = jwt.generate(u.getUsername(), u.getRole());
        return ResponseEntity.ok(Map.of(
            "token", token, "username", u.getUsername(),
            "fullName", u.getFullName(), "role", u.getRole(), "id", u.getId()
        ));
    }

    public ResponseEntity<?> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsernameAndActiveTrue(username)
            .map(u -> ResponseEntity.ok(Map.of(
                "username", u.getUsername(), "fullName", u.getFullName(),
                "role", u.getRole(), "id", u.getId()
            )))
            .orElse(ResponseEntity.status(401).build());
    }

    @jakarta.annotation.PostConstruct
    public void seedAdmin() {
        if (!userRepo.existsByUsername("admin")) {
            userRepo.save(UserEntity.builder()
                .username("admin").fullName("Administrator").role("ADMIN").active(true)
                .passwordHash(encoder.encode("admin123")).build());
        }
    }
}
