package com.poly.models.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CategoryRequest {

	private Long id;

	@NotBlank(message = "Category name is required")
	private String name;

	private String description;
}
