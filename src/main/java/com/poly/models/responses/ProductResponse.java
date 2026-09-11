package com.poly.models.responses;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ProductResponse {
	
	private String id;
	
	private String name;

	private String description;
	
	private String price;
	
	private String createdDate;
	
	private String customised;

	private String available;
	
	private String quantity;
	
	private String sales;
	
	private String categoryId;
	
	
	private List<ProductImageResponse> productImageResponses;
	
	private List<CommentResponse> commentResponses;

	private List<DiscountResponse> discountResponses;

}
