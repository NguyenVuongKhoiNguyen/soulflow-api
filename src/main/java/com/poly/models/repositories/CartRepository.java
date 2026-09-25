package com.poly.models.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
	List<Cart> findByExpiredDateBetweenOrderByIdAsc(LocalDateTime fromDate, LocalDateTime toDate);

	@Query("""
		SELECT ca
		FROM Cart ca
		WHERE (
				:keyword IS NULL
				OR LOWER(ca.account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(ca.account.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR STR(ca.id) LIKE CONCAT('%', :keyword, '%')
			)
			AND (:accountId IS NULL OR ca.account.id = :accountId)
			AND (:fromDate IS NULL OR ca.createdDate >= :fromDate)
			AND (:toDate IS NULL OR ca.createdDate <= :toDate)
			AND (:expired IS NULL OR ca.expired = :expired)
			AND (:deleted IS NULL OR ca.deleted = :deleted)
    """)
	Page<Cart> filterCarts(
            @Param("accountId") Long accountId,
			@Param("keyword") String keyword,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate,
			@Param("expired") Boolean expired,
			@Param("deleted") Boolean deleted,
			Pageable pageable
	);
	
	@Modifying
	@Transactional
	@Query("""
			UPDATE Cart ca
			SET ca.expired = true
			WHERE (
					:keyword IS NULL
					OR STR(ca.id) LIKE CONCAT('%', :keyword, '%')
					OR LOWER(ca.account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(ca.account.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
					OR LOWER(ca.account.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
				)
				AND (:fromDate IS NULL OR ca.createdDate >= :fromDate)
				AND (:toDate IS NULL OR ca.createdDate <= :toDate)
				AND (:expired IS NULL OR ca.expired = :expired)
				AND (:deleted IS NULL OR ca.deleted = :deleted)
				AND ca.expiredDate <= CURRENT_TIMESTAMP
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
    @Query("UPDATE Cart ca SET ca.expired = true WHERE ca.id = :id AND ca.expired = false AND ca.expiredDate <= CURRENT_TIMESTAMP")
    int checkAndExpire(@Param("id") Long id);
	
	@Modifying
    @Transactional
    @Query("UPDATE Cart ca SET ca.deleted = true WHERE ca.id = :id")
    int softDelete(@Param("id") Long id);
}
