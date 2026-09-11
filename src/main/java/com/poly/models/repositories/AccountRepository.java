package com.poly.models.repositories;

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

import com.poly.models.entities.Account;
import com.poly.models.enums.RoleCode;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

	List<Account> findByUsernameStartingWithOrderByUsernameAsc(String usernamePrefix);
	
	Optional<Account> findByEmail(String email); 
		
	Optional<Account> findByUsername(String username);
	
	@Query("""
		SELECT DISTINCT a
		FROM Account a
		LEFT JOIN a.roles r
		WHERE
			(:deleted IS NULL OR a.deleted = :deleted)
			AND (
				:keyword IS NULL
				OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(a.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
			)
			AND (:fromDate IS NULL OR a.createdDate >= :fromDate)
			AND (:toDate IS NULL OR a.createdDate <= :toDate)
			AND (:disabled IS NULL OR a.disabled = :disabled)
			AND (:role IS NULL OR r.code = :role)
	""")
	Page<Account> filterAccounts(
		@Param("keyword") String keyword,
		@Param("deleted") Boolean deleted,
		@Param("fromDate") LocalDateTime fromDate,
		@Param("toDate") LocalDateTime toDate,
		@Param("disabled") Boolean disabled,
		@Param("role") RoleCode role,
		Pageable pageable
	);
	
	@Modifying
	@Transactional
	@Query("""
		UPDATE Account a
		SET a.credentialExpired = true
		WHERE
			(:deleted is NULL OR a.deleted = :deleted)
			AND (
				:keyword IS NULL
				OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(a.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
			)
			AND (:fromDate IS NULL OR a.createdDate >= :fromDate)
			AND (:toDate IS NULL OR a.createdDate <= :toDate)
			AND (:disabled IS NULL OR a.disabled = :disabled)
			AND a.credentialExpiredDate <= CURRENT_TIMESTAMP
		""")
	void checkAndExpireCredentialBeforePagination(
			@Param("deleted") Boolean deleted,
			@Param("keyword") String keyword,
			@Param("fromDate") LocalDateTime fromDate,
			@Param("toDate") LocalDateTime toDate,
			@Param("disabled") Boolean disabled
	);

	@Modifying
	@Transactional
	@Query("UPDATE Account a SET a.credentialExpired = true WHERE a.username = :username AND a.credentialExpiredDate <= CURRENT_TIMESTAMP")
	void checkAndExpireCredential(@Param("username") String username);
	
	@Modifying
    @Transactional
    @Query("UPDATE Account a SET a.deleted = true WHERE a.id = :id")
    void softDelete(@Param("id") Long id);
}
