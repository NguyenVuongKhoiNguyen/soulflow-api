package com.poly.models.requests;

import java.util.List;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class CartRequest {
    private Long id;
    private Long accountId;
    
    @Valid
    private List<ItemRequest> itemRequests;
}