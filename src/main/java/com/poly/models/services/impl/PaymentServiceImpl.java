package com.poly.models.services.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.poly.models.entities.Payment;
import com.poly.models.mappers.PaymentMapper;
import com.poly.models.repositories.PaymentRepository;
import com.poly.models.requests.PaymentRequest;
import com.poly.models.responses.PaymentResponse;
import com.poly.models.services.PaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRepository paymentRepo;
    
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
	@Caching(evict = {
		@CacheEvict(value = "orderList", key = "#request.orderId"),
		@CacheEvict(value = "orderPages", allEntries = true)
	})
    public PaymentResponse save(PaymentRequest request) {
        Payment payment = paymentMapper.toEntity(request);
        paymentRepo.findByOrderId(request.getOrderId()).ifPresent(existing -> {
            payment.setId(existing.getId());
            payment.setPaymentDate(existing.getPaymentDate());
            if (request.getPaymentMethod() == null) {
                payment.setPaymentMethod(existing.getPaymentMethod());
            }
            if (request.getAmount() == null) {
                payment.setAmount(existing.getAmount());
            }
            if (request.getPaid() == null) {
                payment.setPaid(existing.getPaid());
            }
        });
        Payment saved = paymentRepo.save(payment);
        return paymentMapper.toResponse(saved);
    }
}
