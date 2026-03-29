package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String username;
    @Column(nullable = false) private String passwordHash;
    private String fullName;
    @Builder.Default private String role = "USER";
    @Builder.Default private boolean active = true;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
