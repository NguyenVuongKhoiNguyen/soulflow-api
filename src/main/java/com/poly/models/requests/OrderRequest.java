package com.poly.models.requests;

import java.math.BigDecimal;
import java.util.List;

import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import lombok.Data;

@Data
public class OrderRequest {

    private Long id;
    
    @NotBlank(message = "Fullname is required")
    private String fullname;
    
    @NotBlank(message = "Phone number is required")
    private String phone;
    
    @NotBlank(message = "Address is required")
    private String address;

    @DecimalMin(value = "0.0", message = "Shipping fee cannot be negative")
    private BigDecimal shippingFee = BigDecimal.ZERO;
    
    private OrderStatus status;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private Long accountId;

    private Long storeId;
    
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderDetailRequest> orderDetailRequests;  
}
