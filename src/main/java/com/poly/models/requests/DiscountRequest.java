package com.poly.models.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DiscountRequest {

    private Long id;
	
    @NotNull(message = "Percentage is required")
    @Min(value = 0, message = "Percentage cannot be less than 0")
    @Max(value = 100, message = "Percentage cannot be greater than 100")
	private	BigDecimal percentage;
	
	private String description;
	
	private LocalDateTime expiredDate;

}
