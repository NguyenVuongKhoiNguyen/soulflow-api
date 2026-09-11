package com.poly.models.responses;

import lombok.Data;

@Data
public class PaymentResponse {

    private String id;

    private String paid;

    private String amount;

    private String paymentMethod;

    private String paymentDate;

    private OrderResponse orderResponse;
}
