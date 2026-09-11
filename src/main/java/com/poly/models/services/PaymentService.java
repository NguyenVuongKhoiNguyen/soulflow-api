package com.poly.models.services;

import com.poly.models.requests.PaymentRequest;
import com.poly.models.responses.PaymentResponse;

public interface PaymentService {

    PaymentResponse save(PaymentRequest request);
} 
