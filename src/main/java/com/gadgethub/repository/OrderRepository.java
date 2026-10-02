package com.gadgethub.repository;

import com.gadgethub.model.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("select count(i) from OrderItem i where i.product.id = :productId")
    long countItemsByProductId(@Param("productId") Long productId);
}
