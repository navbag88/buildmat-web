package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "products")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    private String category;
    @Column(nullable = false) private String unit;
    @Column(nullable = false) private Double price;
    @Builder.Default private Double stockQty = 0.0;
    @Builder.Default private Double sgstPercent = 0.0;
    @Builder.Default private Double cgstPercent = 0.0;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
