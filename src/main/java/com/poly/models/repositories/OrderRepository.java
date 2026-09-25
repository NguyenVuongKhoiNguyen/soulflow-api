package com.poly.models.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Order;
import com.poly.models.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = """
        SELECT CONVERT(varchar(10), created_date, 23) AS period,
               COUNT(*) AS orderCount,
               COALESCE(SUM(total), 0) AS revenue
        FROM orders
        WHERE del_if = 0
          AND status <> 'CANCELLED'
          AND created_date >= :fromDate
          AND created_date < :toDateExclusive
        GROUP BY CONVERT(varchar(10), created_date, 23)
        ORDER BY CONVERT(varchar(10), created_date, 23)
        """, nativeQuery = true)
    List<DashboardPeriodProjection> summarizeByDay(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDateExclusive") LocalDateTime toDateExclusive
    );

    @Query(value = """
        SELECT CONCAT(CAST(DATEPART(YEAR, created_date) AS varchar(4)), '-Q',
                      CAST(DATEPART(QUARTER, created_date) AS varchar(1))) AS period,
               COUNT(*) AS orderCount,
               COALESCE(SUM(total), 0) AS revenue
        FROM orders
        WHERE del_if = 0
          AND status <> 'CANCELLED'
          AND DATEPART(YEAR, created_date) BETWEEN :fromYear AND :toYear
        GROUP BY DATEPART(YEAR, created_date), DATEPART(QUARTER, created_date)
        ORDER BY DATEPART(YEAR, created_date), DATEPART(QUARTER, created_date)
        """, nativeQuery = true)
    List<DashboardPeriodProjection> summarizeByQuarter(
        @Param("fromYear") Integer fromYear,
        @Param("toYear") Integer toYear
    );

    @Query(value = """
        SELECT CAST(DATEPART(YEAR, created_date) AS varchar(4)) AS period,
               COUNT(*) AS orderCount,
               COALESCE(SUM(total), 0) AS revenue
        FROM orders
        WHERE del_if = 0
          AND status <> 'CANCELLED'
          AND DATEPART(YEAR, created_date) BETWEEN :fromYear AND :toYear
        GROUP BY DATEPART(YEAR, created_date)
        ORDER BY DATEPART(YEAR, created_date)
        """, nativeQuery = true)
    List<DashboardPeriodProjection> summarizeByYear(
        @Param("fromYear") Integer fromYear,
        @Param("toYear") Integer toYear
    );

    @Query(value = """
        SELECT COALESCE(category_data.name, N'Uncategorized') AS categoryName, SUM(details.quantity) AS sales
        FROM orders order_data
        JOIN orders_details details ON details.order_id = order_data.id
        LEFT JOIN products product_data ON product_data.id = details.product_id
        LEFT JOIN categories category_data ON category_data.id = product_data.category_id
        WHERE order_data.del_if = 0
          AND order_data.status <> 'CANCELLED'
          AND order_data.created_date >= :fromDate
          AND order_data.created_date < :toDateExclusive
        GROUP BY COALESCE(category_data.name, N'Uncategorized')
        ORDER BY SUM(details.quantity) DESC, COALESCE(category_data.name, N'Uncategorized')
        """, nativeQuery = true)
    List<DashboardCategorySalesProjection> summarizeCategorySalesByDay(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDateExclusive") LocalDateTime toDateExclusive
    );

    @Query(value = """
        SELECT COALESCE(category_data.name, N'Uncategorized') AS categoryName, SUM(details.quantity) AS sales
        FROM orders order_data
        JOIN orders_details details ON details.order_id = order_data.id
        LEFT JOIN products product_data ON product_data.id = details.product_id
        LEFT JOIN categories category_data ON category_data.id = product_data.category_id
        WHERE order_data.del_if = 0
          AND order_data.status <> 'CANCELLED'
          AND DATEPART(YEAR, order_data.created_date) BETWEEN :fromYear AND :toYear
        GROUP BY COALESCE(category_data.name, N'Uncategorized')
        ORDER BY SUM(details.quantity) DESC, COALESCE(category_data.name, N'Uncategorized')
        """, nativeQuery = true)
    List<DashboardCategorySalesProjection> summarizeCategorySalesByYearRange(
        @Param("fromYear") Integer fromYear,
        @Param("toYear") Integer toYear
    );

    @Query(value = """
        SELECT details.product_name AS productName, SUM(details.quantity) AS sales
        FROM orders order_data
        JOIN orders_details details ON details.order_id = order_data.id
        WHERE order_data.del_if = 0
          AND order_data.status <> 'CANCELLED'
          AND order_data.created_date >= :fromDate
          AND order_data.created_date < :toDateExclusive
        GROUP BY details.product_name
        ORDER BY SUM(details.quantity) DESC, details.product_name
        """, nativeQuery = true)
    List<DashboardProductSalesProjection> summarizeProductSalesByDay(
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDateExclusive") LocalDateTime toDateExclusive
    );

    @Query(value = """
        SELECT details.product_name AS productName, SUM(details.quantity) AS sales
        FROM orders order_data
        JOIN orders_details details ON details.order_id = order_data.id
        WHERE order_data.del_if = 0
          AND order_data.status <> 'CANCELLED'
          AND DATEPART(YEAR, order_data.created_date) BETWEEN :fromYear AND :toYear
        GROUP BY details.product_name
        ORDER BY SUM(details.quantity) DESC, details.product_name
        """, nativeQuery = true)
    List<DashboardProductSalesProjection> summarizeProductSalesByYearRange(
        @Param("fromYear") Integer fromYear,
        @Param("toYear") Integer toYear
    );

    @Query("SELECT o FROM Order o WHERE o.account.username = :username AND o.deleted = false")
    Page<Order> findActiveByUsername(@Param("username") String username, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.account.username = :username AND o.deleted = false")
    Optional<Order> findActiveByIdAndUsername(@Param("id") Long id, @Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findForPayment(@Param("id") Long id);

	List<Order> findByAddressStartingWithOrderByIdAsc(String addressPrefix);

    List<Order> findByAddressIn(List<String> addresses);
	
    @Query("""
        SELECT o
        FROM Order o
        LEFT JOIN o.account account
        WHERE
            (:deleted IS NULL OR o.deleted = :deleted)
            AND (:expired IS NULL OR o.expired = :expired)
            AND (
                :keyword IS NULL
                OR STR(o.id) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
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
                    OR STR(o.id) LIKE CONCAT('%', :keyword, '%')
                    OR LOWER(o.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(o.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR EXISTS (
                        SELECT 1 FROM Account account
                        WHERE account = o.account
                        AND (
                            LOWER(account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(account.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(account.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                    )
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE orders SET status = 'DELIVERED' WHERE id IN (:orderIds)", nativeQuery = true)
    int markSeededOrdersAsDelivered(@Param("orderIds") List<Long> orderIds);
}
