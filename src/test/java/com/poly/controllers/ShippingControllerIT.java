package com.poly.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.poly.models.services.GeocodingService;
import com.poly.models.services.GeocodingService.Coordinate;
import com.poly.models.services.OsrmService;

class ShippingControllerIT extends BaseControllerIT {

    @MockBean
    private OsrmService osrmService;

    @MockBean
    private GeocodingService geocodingService;

    // TC_SHIP_01: Tính khoảng cách lái xe thành công theo tọa độ GPS
    @Test
    @DisplayName("GET /shipping/distance - tính khoảng cách theo tọa độ thành công 200 OK")
    void calculateDrivingDistance_validCoordinates_returns200() throws Exception {
        when(osrmService.calculateDrivingDistanceMeters(any(Coordinate.class), any(Coordinate.class)))
                .thenReturn(5_000L);

        mockMvc.perform(get("/shipping/distance")
                .param("originLatitude", "10.7769")
                .param("originLongitude", "106.7009")
                .param("destinationLatitude", "10.7800")
                .param("destinationLongitude", "106.7050")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceMeters").value(5000))
                .andExpect(jsonPath("$.distanceKilometers").value(5.0));
    }

    // TC_SHIP_02: Thất bại khi thiếu tham số bắt buộc (originLongitude)
    @Test
    @DisplayName("GET /shipping/distance - thiếu tham số trả về 400 Bad Request")
    void calculateDrivingDistance_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/shipping/distance")
                .param("originLatitude", "10.7769")
                .param("destinationLatitude", "10.7800")
                .param("destinationLongitude", "106.7050"))
                .andExpect(status().isBadRequest());
    }

    // TC_SHIP_03: Thất bại khi tham số tọa độ sai kiểu dữ liệu ('abc')
    @Test
    @DisplayName("GET /shipping/distance - sai kiểu dữ liệu trả về 400 Bad Request")
    void calculateDrivingDistance_typeMismatch_returns400() throws Exception {
        mockMvc.perform(get("/shipping/distance")
                .param("originLatitude", "abc")
                .param("originLongitude", "106.7009")
                .param("destinationLatitude", "10.7800")
                .param("destinationLongitude", "106.7050"))
                .andExpect(status().isBadRequest());
    }

    // TC_SHIP_04: Tọa độ trùng nhau trả về 0 mét
    @Test
    @DisplayName("GET /shipping/distance - tọa độ trùng nhau trả về distance = 0")
    void calculateDrivingDistance_sameCoordinates_returnsZeroDistance() throws Exception {
        when(osrmService.calculateDrivingDistanceMeters(any(Coordinate.class), any(Coordinate.class)))
                .thenReturn(0L);

        mockMvc.perform(get("/shipping/distance")
                .param("originLatitude", "10.7769")
                .param("originLongitude", "106.7009")
                .param("destinationLatitude", "10.7769")
                .param("destinationLongitude", "106.7009"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceMeters").value(0))
                .andExpect(jsonPath("$.distanceKilometers").value(0.0));
    }

    // TC_SHIP_05 / TC_SHIP_08: Tính khoảng cách theo địa chỉ tiếng Việt (công khai 200 OK)
    @Test
    @DisplayName("GET /shipping/distance/by-address - công khai trả về 200 OK")
    void calculateDrivingDistanceByAddress_validAddresses_returns200() throws Exception {
        Coordinate c1 = new Coordinate(10.7769, 106.7009);
        Coordinate c2 = new Coordinate(10.7800, 106.7050);
        when(geocodingService.geocodeVietnameseAddress("123 Lê Lợi")).thenReturn(c1);
        when(geocodingService.geocodeVietnameseAddress("456 Nguyễn Huệ")).thenReturn(c2);
        when(osrmService.calculateDrivingDistanceMeters(c1, c2)).thenReturn(3_000L);

        mockMvc.perform(get("/shipping/distance/by-address")
                .param("originAddress", "123 Lê Lợi")
                .param("destinationAddress", "456 Nguyễn Huệ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceMeters").value(3000))
                .andExpect(jsonPath("$.distanceKilometers").value(3.0));
    }

    // TC_SHIP_06: Thiếu tham số địa chỉ trả về 400 Bad Request
    @Test
    @DisplayName("GET /shipping/distance/by-address - thiếu tham số trả về 400 Bad Request")
    void calculateDrivingDistanceByAddress_missingAddress_returns400() throws Exception {
        mockMvc.perform(get("/shipping/distance/by-address")
                .param("originAddress", "123 Lê Lợi"))
                .andExpect(status().isBadRequest());
    }
}
