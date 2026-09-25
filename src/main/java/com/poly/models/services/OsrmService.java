package com.poly.models.services;

import com.poly.models.services.GeocodingService.Coordinate;

public interface OsrmService {
    long calculateDrivingDistanceMeters(Coordinate origin, Coordinate destination);
}
