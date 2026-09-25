package com.poly.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.poly.models.responses.ShippingDistanceResponse;
import com.poly.models.services.GeocodingService;
import com.poly.models.services.GeocodingService.Coordinate;
import com.poly.models.services.OsrmService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final OsrmService osrmService;
    private final GeocodingService geocodingService;

    @GetMapping("/distance")
    public ResponseEntity<ShippingDistanceResponse> calculateDrivingDistance(
            @RequestParam double originLatitude,
            @RequestParam double originLongitude,
            @RequestParam double destinationLatitude,
            @RequestParam double destinationLongitude) {
        return ResponseEntity.ok(toResponse(osrmService.calculateDrivingDistanceMeters(
            new Coordinate(originLatitude, originLongitude),
            new Coordinate(destinationLatitude, destinationLongitude)
        )));
    }

    @GetMapping("/distance/by-address")
    public ResponseEntity<ShippingDistanceResponse> calculateDrivingDistanceByAddress(
            @RequestParam String originAddress,
            @RequestParam String destinationAddress) {
        return ResponseEntity.ok(toResponse(osrmService.calculateDrivingDistanceMeters(
            geocodingService.geocodeVietnameseAddress(originAddress),
            geocodingService.geocodeVietnameseAddress(destinationAddress)
        )));
    }

    private ShippingDistanceResponse toResponse(long distanceMeters) {
        return new ShippingDistanceResponse(distanceMeters, distanceMeters / 1_000.0);
    }
}
