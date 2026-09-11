package com.poly.models.responses;

import java.util.List;

import lombok.Data;

@Data
public class DiscountResponse {

	private String id;
	
	private	String code;
	
	private	String percentage;
	
	private String description;
	
    private String createdDate;

	private String expiredDate;
	
	private String expired;
	
	private List<ProductResponse> productResponses;
}
