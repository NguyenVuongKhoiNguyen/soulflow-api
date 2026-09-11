package com.poly.models.requests;

import lombok.Data;

@Data
public class OrderDetailRequest {
    private Long id;
    private Integer quantity;
    private Long productId;
}
