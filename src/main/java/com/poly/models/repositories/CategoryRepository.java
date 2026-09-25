package com.poly.models.repositories;

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

import com.poly.models.entities.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByDeletedFalse();

	Optional<Category> findFirstByNameIgnoreCase(String name);

	List<Category> findByDeletedFalseOrderByNameAsc();

	@Query("""
        SELECT c
        FROM Category c
        WHERE
            (:deleted IS NULL OR c.deleted = :deleted) 
            AND (
                :keyword IS NULL
                OR STR(c.id) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
    """)
    Page<Category> filterCategories(
            @Param("keyword") String keyword,
            @Param("deleted") Boolean deleted,
            Pageable pageable
    );
	
	@Modifying
    @Transactional
    @Query("UPDATE Category c SET c.deleted = true WHERE c.id = :id")
    void softDelete(@Param("id") Long id);
}
