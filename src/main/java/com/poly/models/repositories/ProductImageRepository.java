package com.poly.models.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    boolean existsByProductIdAndName(Long productId, String name);
	
	@Modifying
    @Transactional
    @Query("UPDATE ProductImage pi SET pi.deleted = true WHERE pi.id = :id")
    void softDelete(@Param("id") Long id);

    @Query("""
        SELECT pi FROM ProductImage pi
        JOIN pi.product p
        WHERE pi.deleted = false
            AND (:keyword IS NULL
                OR LOWER(pi.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR STR(p.id) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<ProductImage> filterProductImages(
        @Param("keyword") String keyword,
        Pageable pageable
    );
}
