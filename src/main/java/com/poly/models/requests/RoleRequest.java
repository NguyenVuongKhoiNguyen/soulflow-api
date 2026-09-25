package com.poly.models.requests;

import com.poly.models.enums.RoleCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleRequest {
	@NotNull(message = "Role code is required")
	private RoleCode code;
}
