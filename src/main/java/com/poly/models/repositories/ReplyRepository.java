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
                :keyword IS NULL
                OR LOWER(r.account.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.account.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.account.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (:fromDate IS NULL OR r.createdDate >= :fromDate)
            AND (:toDate IS NULL OR r.createdDate <= :toDate)
            AND (:accountId IS NULL OR r.account.id = :accountId)
            AND (:commentId IS NULL OR r.comment.id = :commentId)
    """)
    Page<Reply> filterReplies(
            @Param("keyword") String keyword,
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
