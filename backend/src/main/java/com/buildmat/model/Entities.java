package com.buildmat.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ── User ──────────────────────────────────────────────────────────────────────
@Entity @Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String username;
    @Column(nullable = false) private String passwordHash;
    private String fullName;
    @Builder.Default private String role = "USER";
    @Builder.Default private boolean active = true;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}

// ── Customer ──────────────────────────────────────────────────────────────────
@Entity @Table(name = "customers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class CustomerEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    private String phone;
    private String email;
    @Column(columnDefinition = "TEXT") private String address;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}

// ── Product ───────────────────────────────────────────────────────────────────
@Entity @Table(name = "products")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class ProductEntity {
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

// ── Invoice ───────────────────────────────────────────────────────────────────
@Entity @Table(name = "invoices")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class InvoiceEntity {
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

// ── InvoiceItem ───────────────────────────────────────────────────────────────
@Entity @Table(name = "invoice_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class InvoiceItemEntity {
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

// ── Payment ───────────────────────────────────────────────────────────────────
@Entity @Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class PaymentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id") private InvoiceEntity invoice;
    @Column(nullable = false) private Double amount;
    @Column(nullable = false) private LocalDate paymentDate;
    @Builder.Default private String method = "CASH";
    private String reference;
    private String notes;
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
