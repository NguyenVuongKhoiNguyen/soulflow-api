package com.poly.models.requests;

import java.util.List;

import lombok.Data;

@Data
public class CartRequest {
    private Long id;
    private Long accountId;
    private List<ItemRequest> itemRequests;
}