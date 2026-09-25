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

import com.poly.models.entities.Reply;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByCommentIdInAndContentStartingWith(List<Long> commentIds, String contentPrefix);

    @Query("""
        SELECT r
        FROM Reply r
        WHERE
            (:deleted IS NULL OR r.deleted = :deleted)
            AND (
                :productSearch IS NULL
                OR CAST(r.comment.product.id AS string) = :productSearch
                OR LOWER(r.comment.product.name) LIKE LOWER(CONCAT('%', :productSearch, '%'))
            )
            AND (
                :commentSearch IS NULL
                OR CAST(r.comment.id AS string) = :commentSearch
                OR LOWER(r.comment.account.username) LIKE LOWER(CONCAT('%', :commentSearch, '%'))
                OR LOWER(r.comment.account.fullname) LIKE LOWER(CONCAT('%', :commentSearch, '%'))
                OR LOWER(r.comment.account.email)    LIKE LOWER(CONCAT('%', :commentSearch, '%'))
            )
            AND (
                :replySearch IS NULL
                OR CAST(r.id AS string) = :replySearch
                OR CAST(r.account.id AS string) = :replySearch
                OR LOWER(r.account.username) LIKE LOWER(CONCAT('%', :replySearch, '%'))
                OR LOWER(r.account.fullname) LIKE LOWER(CONCAT('%', :replySearch, '%'))
                OR LOWER(r.account.email)    LIKE LOWER(CONCAT('%', :replySearch, '%'))
            )
            AND (:commentId IS NULL OR r.comment.id = :commentId)
            AND (:accountId IS NULL OR r.account.id = :accountId)
            AND (:fromDate IS NULL OR r.createdDate >= :fromDate)
            AND (:toDate IS NULL OR r.createdDate <= :toDate)
    """)
    Page<Reply> filterReplies(
            @Param("productSearch") String productSearch,
            @Param("commentSearch") String commentSearch,
            @Param("replySearch") String replySearch,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("accountId") Long accountId,
            @Param("commentId") Long commentId,
            @Param("deleted") Boolean deleted,
            Pageable pageable
    );
    
    @Modifying
    @Transactional
    @Query("UPDATE Reply r SET r.deleted = true WHERE r.id = :id")
    void softDelete(@Param("id") Long id);
}
