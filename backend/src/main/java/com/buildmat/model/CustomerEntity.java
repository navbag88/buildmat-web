package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "customers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    private String phone;
    private String email;
    @Column(columnDefinition = "TEXT") private String address;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
