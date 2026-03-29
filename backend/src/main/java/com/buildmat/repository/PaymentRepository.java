package com.buildmat.repository;
import com.buildmat.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByOrderByPaymentDateDescIdDesc();
    List<PaymentEntity> findByInvoiceId(Long invoiceId);
}
