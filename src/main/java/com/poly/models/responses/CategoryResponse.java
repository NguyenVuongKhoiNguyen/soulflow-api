package com.poly.models.responses;

import java.util.List;

import lombok.Data;

@Data
public class CategoryResponse {
	
	private String id;
	
	private String name;

	private String description;
	
	private List<ProductResponse> productResponses;
}
