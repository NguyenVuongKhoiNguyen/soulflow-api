package com.poly.models.repositories;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Order;
import com.poly.models.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
	
	
    @Query("""
        SELECT o
        FROM Order o
        WHERE
            (:deleted IS NULL OR o.deleted = :deleted)
            AND (:expired IS NULL OR o.expired = :expired)
            AND (
                :keyword IS NULL
                OR STR(o.id) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(o.account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(o.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:fromDate IS NULL OR o.createdDate >= :fromDate)
            AND (:toDate IS NULL OR o.createdDate <= :toDate)
            AND (:status IS NULL OR o.status = :status)
    """)
    Page<Order> filterOrders(
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("status") OrderStatus status,
            @Param("expired") Boolean expired,
            @Param("deleted") Boolean deleted,
            Pageable pageable
    );
    
    @Modifying
    @Transactional
    @Query("""
            UPDATE Order o 
            SET o.expired = true
            WHERE
                (:deleted IS NULL OR o.deleted = :deleted)
                AND (:expired IS NULL OR o.expired = :expired)
                AND (
                    :keyword IS NULL
                    OR LOWER(o.account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(o.account.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(o.account.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                )
                AND (:fromDate IS NULL OR o.createdDate >= :fromDate)
                AND (:toDate IS NULL OR o.createdDate <= :toDate)
                AND o.expiredDate <= CURRENT_TIMESTAMP
        """)
    int checkAndExpireBeforePagination(
    		@Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("expired") Boolean expired,
            @Param("deleted") Boolean deleted
    );
    
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.expired = true WHERE o.id = :id AND o.expired = false AND o.expiredDate <= CURRENT_TIMESTAMP")
    int checkAndExpire(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.deleted = true WHERE o.id = :id")
    int softDelete(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE orders
        SET status = 'PAID'
        WHERE id = :orderId
        AND (
            SELECT COALESCE(SUM(p.amount), 0)
            FROM payments p
            WHERE p.order_id = :orderId
            AND p.paid = 1
        ) >= total
    """, nativeQuery = true)
    int markOrderAsPaidIfFullyPaid(@Param("orderId") Long orderId);
}
