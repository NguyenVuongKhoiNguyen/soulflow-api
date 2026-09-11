package com.poly.models.requests;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductRequest {
	private Long id;
	private String name;
	private String description;
	private BigDecimal price;
	private Boolean customised;
	private Boolean available;
	private Integer quantity;
	private Long categoryId;
}
