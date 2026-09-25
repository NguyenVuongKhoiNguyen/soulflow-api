package com.poly.models.requests;

import java.math.BigDecimal;

import com.poly.models.enums.OrderStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminOrderUpdateRequest {

    @NotBlank(message = "Fullname is required")
    private String fullname;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Address is required")
    private String address;

    @DecimalMin(value = "0.0", message = "Shipping fee cannot be negative")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    private Long storeId;

    @NotNull(message = "Order status is required")
    private OrderStatus status;
}
