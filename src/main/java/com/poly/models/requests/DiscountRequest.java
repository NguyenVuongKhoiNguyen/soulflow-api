package com.poly.models.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DiscountRequest {

    private Long id;
	
	private	BigDecimal percentage;
	
	private String description;
	
	private LocalDateTime expiredDate;

}
