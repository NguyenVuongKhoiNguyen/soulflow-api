package com.poly.models.repositories;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	Optional<Product> findFirstByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

	@Query("""
		SELECT p
		FROM Product p
		LEFT JOIN FETCH p.category c
		WHERE p.deleted = false
			AND p.available = true
			AND p.quantity > 0
			AND (:keyword IS NULL
				OR STR(p.id) LIKE CONCAT('%', :keyword, '%')
				OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
			AND (:category IS NULL
				OR STR(c.id) LIKE CONCAT('%', :category, '%')
				OR LOWER(c.name) LIKE LOWER(CONCAT('%', :category, '%')))
			AND (:minPrice IS NULL OR p.price >= :minPrice)
			AND (:maxPrice IS NULL OR p.price <= :maxPrice)
			AND (:customised IS NULL OR p.customised = :customised)
		ORDER BY p.sales DESC, p.name ASC
	""")
	List<Product> searchAvailableForAi(
			@Param("keyword") String keyword,
			@Param("category") String category,
			@Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice,
			@Param("customised") Boolean customised,
			Pageable pageable
	);

	@Query("""
		SELECT p
		FROM Product p
		LEFT JOIN FETCH p.category
		WHERE p.deleted = false AND p.id = :id
	""")
	Optional<Product> findActiveByIdForAi(@Param("id") Long id);
	
	@Query("""
        SELECT p
		FROM Product p
		WHERE
			(:deleted IS NULL OR p.deleted = :deleted)
			AND (:available IS NULL OR p.available = :available)
			AND (:customised IS NULL OR p.customised = :customised)

			AND (
				:filterByCategory = false
				OR p.category.id IN :categoryIds
			)

			AND (:minPrice IS NULL OR p.price >= :minPrice)
			AND (:maxPrice IS NULL OR p.price <= :maxPrice)

			AND (
				:keyword IS NULL
				OR STR(p.id) LIKE CONCAT('%', :keyword, '%')
				OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
			)

			AND (:fromDate IS NULL OR p.createdDate >= :fromDate)
			AND (:toDate IS NULL OR p.createdDate <= :toDate)
    """)
    Page<Product> filterProducts(
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("filterByCategory") boolean filterByCategory,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("keyword") String keyword,
            @Param("customised") Boolean customised,
            @Param("available") Boolean available,
            @Param("deleted") Boolean deleted,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );
	
	@Modifying
    @Transactional
    @Query("UPDATE Product p SET p.deleted = true WHERE p.id = :id")
    void softDelete(@Param("id") Long id);

    @Modifying
	@Transactional
	@Query("UPDATE Product p SET p.quantity = p.quantity + :quantity WHERE p.id = :id")
	int increaseQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Modifying
	@Transactional
	@Query("""
		UPDATE Product p
		SET p.quantity = p.quantity - :amount,
		    p.available = CASE WHEN (p.quantity - :amount) = 0 THEN false ELSE p.available END
		WHERE p.id = :id
		AND p.quantity >= :amount
	""")
	int decreaseQuantity(@Param("id") Long id, @Param("amount") Integer amount); 


    @Modifying
    @Transactional
    @Query("""
            UPDATE Product p
            SET p.sales = p.sales + :quantity
            WHERE p.id = :id
            """)
    int increaseSales(@Param("id") Long id, @Param("quantity") Integer quantity);
}
