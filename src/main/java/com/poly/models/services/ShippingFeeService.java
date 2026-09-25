package com.poly.models.services;

import java.math.BigDecimal;

public interface ShippingFeeService {

    BigDecimal calculateFee(String storeAddress, String deliveryAddress);
}
