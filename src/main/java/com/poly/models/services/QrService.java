package com.poly.models.services;

import java.math.BigDecimal;

public interface QrService {
    String buildQrUrl(BigDecimal amount, String orderCode);
} 
