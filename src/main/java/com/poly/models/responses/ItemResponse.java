package com.poly.models.responses;

import lombok.Data;

@Data
public class ItemResponse {
    
    private String id;
    
    private String name;
    
    private String price;
    
    private String quantity;
    
    private String subtotal;
    
    private String productId;

    private String imageUrl;
    
    private String cartId;
}
