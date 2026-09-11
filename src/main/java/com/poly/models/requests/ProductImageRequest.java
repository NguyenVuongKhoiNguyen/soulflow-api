package com.poly.models.requests;

import lombok.Data;

@Data
public class ProductImageRequest {

	private Long id;
	
	private String name;
	
	private Long productId;
}
