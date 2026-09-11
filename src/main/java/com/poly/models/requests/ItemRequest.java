package com.poly.models.requests;

import lombok.Data;

@Data
public class ItemRequest {
    private Long id;
    private Integer quantity;
    private Long productId;
}
