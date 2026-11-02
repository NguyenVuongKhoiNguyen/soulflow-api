package com.poly.models.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.poly.models.services.GeocodingService.Coordinate;
import com.poly.models.services.impl.ShippingFeeServiceImpl;

@ExtendWith(MockitoExtension.class)
class ShippingFeeServiceTest {

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private OsrmService osrmService;

    @InjectMocks
    private ShippingFeeServiceImpl shippingFeeService;

    private Coordinate originCoord;
    private Coordinate destCoord;

    @BeforeEach
    void setUp() {
        originCoord = new Coordinate(10.7769, 106.7009);
        destCoord = new Coordinate(10.7800, 106.7050);
    }

    // TC_SHIP_01: Tính phí vận chuyển trong cự ly cơ bản (<= 2km)
    @Test
    @DisplayName("calculateFee - cự ly <= 2km trả về cước cơ sở 13,000 VND")
    void calculateFee_withinBaseDistance_returnsBaseFee() {
        when(geocodingService.geocodeVietnameseAddress("Store Q1")).thenReturn(originCoord);
        when(geocodingService.geocodeVietnameseAddress("Delivery Q1")).thenReturn(destCoord);
        when(osrmService.calculateDrivingDistanceMeters(originCoord, destCoord)).thenReturn(1_500L); // 1.5km

        BigDecimal fee = shippingFeeService.calculateFee("Store Q1", "Delivery Q1");

        assertNotNull(fee);
        assertEquals(BigDecimal.valueOf(13_000), fee);
    }

    // TC_SHIP_01: Tính phí vận chuyển cự ly xa (> 2km)
    @Test
    @DisplayName("calculateFee - cự ly 5.2km (làm tròn lên 6km) cộng thêm phí vượt km")
    void calculateFee_exceedingBaseDistance_addsAdditionalKmFee() {
        when(geocodingService.geocodeVietnameseAddress("Store Q1")).thenReturn(originCoord);
        when(geocodingService.geocodeVietnameseAddress("Delivery Q7")).thenReturn(destCoord);
        when(osrmService.calculateDrivingDistanceMeters(originCoord, destCoord)).thenReturn(5_200L); // 5.2km -> 6km CEILING

        // Base 13,000 + (6 - 2) * 5,000 = 13,000 + 20,000 = 33,000
        BigDecimal fee = shippingFeeService.calculateFee("Store Q1", "Delivery Q7");

        assertNotNull(fee);
        assertEquals(BigDecimal.valueOf(33_000), fee);
    }

    // TC_SHIP_04: Cự ly bằng 0 (điểm đến trùng điểm đi)
    @Test
    @DisplayName("calculateFee - cự ly 0 mét trả về phí cơ sở 13,000 VND")
    void calculateFee_zeroDistance_returnsBaseFee() {
        when(geocodingService.geocodeVietnameseAddress("Store Q1")).thenReturn(originCoord);
        when(geocodingService.geocodeVietnameseAddress("Store Q1")).thenReturn(originCoord);
        when(osrmService.calculateDrivingDistanceMeters(originCoord, originCoord)).thenReturn(0L);

        BigDecimal fee = shippingFeeService.calculateFee("Store Q1", "Store Q1");

        assertNotNull(fee);
        assertEquals(BigDecimal.valueOf(13_000), fee);
    }

    // TC_SHIP_06: Để trống địa chỉ cửa hàng ném ResponseStatusException BAD_REQUEST
    @Test
    @DisplayName("calculateFee - để trống storeAddress ném BAD_REQUEST")
    void calculateFee_blankStoreAddress_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            shippingFeeService.calculateFee("   ", "Delivery Address");
        });
        assertTrue(ex.getMessage().contains("Store and delivery addresses are required"));
    }

    // TC_SHIP_06: Để trống địa chỉ giao hàng ném ResponseStatusException BAD_REQUEST
    @Test
    @DisplayName("calculateFee - để trống deliveryAddress ném BAD_REQUEST")
    void calculateFee_blankDeliveryAddress_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            shippingFeeService.calculateFee("Store Address", "");
        });
        assertTrue(ex.getMessage().contains("Store and delivery addresses are required"));
    }
}
