package com.poly.models.mappers;

import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.poly.models.entities.Role;
import com.poly.models.repositories.RoleRepository;
import com.poly.models.requests.RoleRequest;
import com.poly.models.responses.RoleResponse;

@Mapper(componentModel = "spring")
public abstract class RoleMapper {
	
	@Autowired
	protected RoleRepository roleRepo;

	public Role toEntity(RoleRequest request) {
		if (request == null || request.getCode() == null) return null;
		return roleRepo.findByCode(request.getCode())
				.orElseThrow(() -> new IllegalArgumentException("Unknown role: " + request.getCode()));
	}

	public abstract RoleResponse toResponse(Role role);
}

