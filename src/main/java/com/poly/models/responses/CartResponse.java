package com.poly.models.responses;

import java.util.List;

import lombok.Data;

@Data
public class CartResponse {
    
    private String id;
    
    private String total;
    
    private String createdDate;
    
    private String username;
    
    private String fullname;
    
    private String accountId;
    
    private List<ItemResponse> itemResponses;
}
