package com.buildmat.repository;

import com.buildmat.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsernameAndActiveTrue(String username);
    boolean existsByUsername(String username);
}

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    List<CustomerEntity> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);
}

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String cat);
}

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {
    List<InvoiceEntity> findByOrderByInvoiceDateDescIdDesc();
    List<InvoiceEntity> findByInvoiceNumberContainingIgnoreCaseOrCustomerNameContainingIgnoreCaseOrStatusContainingIgnoreCase(
        String num, String cust, String status);
    @Query("SELECT MAX(i.invoiceNumber) FROM InvoiceEntity i WHERE i.invoiceNumber LIKE :prefix%")
    Optional<String> findMaxInvoiceNumber(String prefix);
    @Query("SELECT COALESCE(SUM(i.paidAmount),0) FROM InvoiceEntity i")
    Double sumPaidAmount();
    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount),0) FROM InvoiceEntity i WHERE i.status <> 'PAID'")
    Double sumOutstanding();
    @Query("SELECT COUNT(i) FROM InvoiceEntity i WHERE i.status = :status")
    Long countByStatus(String status);
}

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItemEntity, Long> {
    List<InvoiceItemEntity> findByInvoiceId(Long invoiceId);
}

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByOrderByPaymentDateDescIdDesc();
    List<PaymentEntity> findByInvoiceId(Long invoiceId);
}
