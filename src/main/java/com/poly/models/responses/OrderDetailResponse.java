package com.poly.models.responses;

import lombok.Data;

@Data
public class OrderDetailResponse {
    
    private String id;
    
    private String name;
    
    private String price;
    
    private String quantity;
    
    private String subtotal;
    
    private String productId;
    
    private String orderId;
}
