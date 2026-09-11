package com.poly.models.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
	List<Comment> findByProductIdAndContentStartingWith(Long productId, String contentPrefix);

	@Query("""
        SELECT co
        FROM Comment co
        WHERE
            (:deleted IS NULL OR co.deleted = :deleted)
            AND (
                :keyword IS NULL
                OR LOWER(co.product.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(co.account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(co.account.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(co.account.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:productId IS NULL OR co.product.id = :productId)
            AND (:accountId IS NULL OR co.account.id = :accountId)
            AND (:fromDate IS NULL OR co.createdDate >= :fromDate)
            AND (:toDate IS NULL OR co.createdDate <= :toDate)
    """)
    Page<Comment> filterComments(
            @Param("keyword") String keyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("productId") Long productId,
            @Param("accountId") Long accountId,
            @Param("deleted") Boolean deleted,
            Pageable pageable
    );

	@Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.deleted = true WHERE c.id = :id")
    void softDelete(@Param("id") Long id);
}
