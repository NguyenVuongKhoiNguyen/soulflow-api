package com.poly.models.services.impl;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import com.poly.models.services.QrService;

@Service
public class QrServiceImpl implements QrService {

    private static final String BANK_ID = "BIDV"; 
    private static final String ACCOUNT_NO = "6011246825"; 
    private static final String ACCOUNT_NAME = "NGUYEN VUONG KHOI NGUYEN";

    @Override 
    public String buildQrUrl(BigDecimal amount, String orderCode) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("Order code is required");
        }

        String paymentAmount;
        try {
            paymentAmount = amount.toBigIntegerExact().toString();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Payment amount must be a whole number of VND", ex);
        }

        return String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
            BANK_ID,
            ACCOUNT_NO,
            paymentAmount,
            URLEncoder.encode(orderCode.trim(), StandardCharsets.UTF_8),
            URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8)
        );
    }
}
