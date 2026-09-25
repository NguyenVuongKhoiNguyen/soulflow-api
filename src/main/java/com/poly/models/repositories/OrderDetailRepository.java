package com.poly.models.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.poly.models.entities.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
	@Modifying
    @Query(value = """
        UPDATE p
        SET p.quantity = p.quantity + od.quantity,
            p.available = 1
        FROM products p
        JOIN orders_details od ON od.product_id = p.id
        JOIN orders o ON o.id = od.order_id
        WHERE o.id = :orderId
        AND o.status != 'CANCELLED'
        """, nativeQuery = true)
    void restoreProductQuantity(@Param("orderId") Long orderId);
}
