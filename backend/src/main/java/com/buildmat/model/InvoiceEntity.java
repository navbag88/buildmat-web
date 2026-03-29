package com.buildmat.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "invoices")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String invoiceNumber;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private CustomerEntity customer;
    @Column(nullable = false) private LocalDate invoiceDate;
    private LocalDate dueDate;
    @Builder.Default private Double subtotal = 0.0;
    @Builder.Default private Double sgstAmount = 0.0;
    @Builder.Default private Double cgstAmount = 0.0;
    @Builder.Default private Double taxAmount = 0.0;
    @Builder.Default private Double totalAmount = 0.0;
    @Builder.Default private Double paidAmount = 0.0;
    @Builder.Default private Boolean includeGst = true;
    @Builder.Default private String status = "UNPAID";
    @Column(columnDefinition = "TEXT") private String notes;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<InvoiceItemEntity> items = new ArrayList<>();
}
