package com.poly.models.requests;

import java.math.BigDecimal;

import com.poly.models.enums.PaymentMethod;

import lombok.Data;

@Data
public class PaymentRequest {

    private Long id;

    private BigDecimal amount;

    private Boolean paid;

    private PaymentMethod paymentMethod;

    private Long orderId;
}
