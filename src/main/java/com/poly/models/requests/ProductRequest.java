package com.poly.models.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {
	private Long id;

	@NotBlank(message = "Product name is required")
	private String name;

	private String description;

	@NotNull(message = "Price is required")
	@Min(value = 0, message = "Price must be greater than or equal to 0")
	private BigDecimal price;

	private Boolean customised;
	private Boolean available;

	@NotNull(message = "Quantity is required")
	@Min(value = 0, message = "Quantity must be greater than or equal to 0")
	private Integer quantity;

	@NotNull(message = "Category ID is required")
	private Long categoryId;
}
