package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "invoice_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id") private InvoiceEntity invoice;
    private Long productId;
    @Column(nullable = false) private String productName;
    private String unit;
    @Column(nullable = false) private Double quantity;
    @Column(nullable = false) private Double unitPrice;
    @Column(nullable = false) private Double total;
    @Builder.Default private Double sgstPercent = 0.0;
    @Builder.Default private Double cgstPercent = 0.0;
}
