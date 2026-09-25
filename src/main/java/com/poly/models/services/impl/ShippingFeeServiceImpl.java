package com.poly.models.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.poly.models.services.GeocodingService;
import com.poly.models.services.OsrmService;
import com.poly.models.services.ShippingFeeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShippingFeeServiceImpl implements ShippingFeeService {

    private static final BigDecimal BASE_FEE = BigDecimal.valueOf(13_000);
    private static final BigDecimal ADDITIONAL_KILOMETER_FEE = BigDecimal.valueOf(5_000);
    private static final BigDecimal BASE_DISTANCE_KILOMETERS = BigDecimal.valueOf(2);

    private final GeocodingService geocodingService;
    private final OsrmService osrmService;

    @Override
    @Cacheable(value = "shippingFees", key = "#storeAddress + '_' + #deliveryAddress")
    public BigDecimal calculateFee(String storeAddress, String deliveryAddress) {
        if (!StringUtils.hasText(storeAddress) || !StringUtils.hasText(deliveryAddress)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Store and delivery addresses are required");
        }

        long distanceMeters = osrmService.calculateDrivingDistanceMeters(
            geocodingService.geocodeVietnameseAddress(storeAddress),
            geocodingService.geocodeVietnameseAddress(deliveryAddress)
        );
        BigDecimal distanceKilometers = BigDecimal.valueOf(distanceMeters)
            .divide(BigDecimal.valueOf(1_000), 0, RoundingMode.CEILING);

        if (distanceKilometers.compareTo(BASE_DISTANCE_KILOMETERS) <= 0) {
            return BASE_FEE;
        }
        return BASE_FEE.add(distanceKilometers.subtract(BASE_DISTANCE_KILOMETERS)
            .multiply(ADDITIONAL_KILOMETER_FEE));
    }
}
