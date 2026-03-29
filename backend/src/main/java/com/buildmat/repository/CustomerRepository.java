package com.buildmat.repository;
import com.buildmat.model.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    List<CustomerEntity> findByNameContainingIgnoreCaseOrPhoneContaining(String name, String phone);
}
