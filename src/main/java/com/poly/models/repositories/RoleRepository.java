package com.poly.models.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.poly.models.entities.Role;
import com.poly.models.enums.RoleCode;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByCode(RoleCode roleCode);
}
