package com.buildmat.repository;
import com.buildmat.model.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

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
