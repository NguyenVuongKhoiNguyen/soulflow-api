package com.poly.models.responses;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class OrderResponse {
    
    private String id;
    
    private String fullname;
    
    private String phone;
    
    private String address;
    
    private String total;

    private String shippingFee;

    private String createdDate;

    private String expiredDate;

    private Boolean expired;
    
    private String status;

    private String paymentMethod;

    private Boolean paid;

    private String paymentReference;

    private String qrUrl;
    
    private String accountId;

    private String storeId;
    
    private List<OrderDetailResponse> orderDetailResponses;
}
