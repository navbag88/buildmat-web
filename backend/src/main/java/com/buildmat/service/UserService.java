package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public List<Map<String,Object>> getAll() { return repo.findAll().stream().map(this::toMap).collect(Collectors.toList()); }

    public Map<String,Object> create(Map<String,Object> body) {
        if (repo.existsByUsername((String)body.get("username"))) throw new RuntimeException("Username already exists");
        UserEntity u = UserEntity.builder()
            .username(((String)body.get("username")).trim().toLowerCase())
            .fullName((String)body.get("fullName"))
            .role((String)body.getOrDefault("role","USER"))
            .active((Boolean)body.getOrDefault("active",true))
            .passwordHash(encoder.encode((String)body.get("password")))
            .createdAt(LocalDateTime.now()).build();
        return toMap(repo.save(u));
    }

    public Map<String,Object> update(Long id, Map<String,Object> body) {
        UserEntity u = repo.findById(id).orElseThrow();
        u.setFullName((String)body.get("fullName"));
        u.setRole((String)body.getOrDefault("role", u.getRole()));
        u.setActive((Boolean)body.getOrDefault("active", u.isActive()));
        return toMap(repo.save(u));
    }

    public void changePassword(Long id, String newPassword) {
        UserEntity u = repo.findById(id).orElseThrow();
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
    }

    public void delete(Long id) { repo.deleteById(id); }

    private Map<String,Object> toMap(UserEntity u) {
        return Map.of("id",u.getId(),"username",u.getUsername(),"fullName",u.getFullName(),
            "role",u.getRole(),"active",u.isActive());
    }
}
