package com.poly.models.repositories;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.poly.models.entities.Store;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByNameIgnoreCase(String name);

    @Query("""
        SELECT s
        FROM Store s
        WHERE (:id IS NULL OR s.id = :id)
          AND (:storeName IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :storeName, '%')))
        """)
    Page<Store> filterStores(
        @Param("id") Long id,
        @Param("storeName") String storeName,
        Pageable pageable
    );
}
