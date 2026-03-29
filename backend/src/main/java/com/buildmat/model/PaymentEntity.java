package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id") private InvoiceEntity invoice;
    @Column(nullable = false) private Double amount;
    @Column(nullable = false) private LocalDate paymentDate;
    @Builder.Default private String method = "CASH";
    private String reference;
    private String notes;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
