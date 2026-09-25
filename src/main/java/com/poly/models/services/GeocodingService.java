package com.poly.models.services;

public interface GeocodingService {
    Coordinate geocodeVietnameseAddress(String address);

    record Coordinate(double latitude, double longitude) {
    }
}
