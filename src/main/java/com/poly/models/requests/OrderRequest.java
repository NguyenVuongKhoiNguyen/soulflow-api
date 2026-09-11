package com.poly.models.requests;

import java.util.List;

import com.poly.models.enums.OrderStatus;
import com.poly.models.enums.PaymentMethod;

import lombok.Data;

@Data
public class OrderRequest {

    private Long id;
    
    private String fullname;
    
    private String phone;
    
    private String address;
    
    private OrderStatus status;

    private PaymentMethod paymentMethod;
    
    private Long accountId;
    
    private List<OrderDetailRequest> orderDetailRequests;  
}
