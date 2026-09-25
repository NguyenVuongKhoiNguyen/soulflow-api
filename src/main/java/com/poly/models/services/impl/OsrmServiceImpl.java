package com.poly.models.services.impl;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.poly.config.OsrmProperties;
import com.poly.models.services.GeocodingService.Coordinate;
import com.poly.models.services.OsrmService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OsrmServiceImpl implements OsrmService {

    private final OsrmProperties osrmProperties;

    @Override
    public long calculateDrivingDistanceMeters(Coordinate origin, Coordinate destination) {
        String coordinates = String.format(Locale.ROOT, "%f,%f;%f,%f",
            origin.longitude(), origin.latitude(), destination.longitude(), destination.latitude());
        try {
            JsonNode response = RestClient.create(osrmProperties.baseUrl())
                .get()
                .uri("/route/v1/driving/{coordinates}?overview=false&alternatives=false&steps=false", coordinates)
                .retrieve()
                .body(JsonNode.class);

            JsonNode distance = response == null ? null : response.path("routes").path(0).path("distance");
            if (distance == null || !distance.isNumber() || distance.asDouble() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not calculate a driving distance for the selected locations");
            }
            return Math.round(distance.asDouble());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Hosted OSRM request failed: {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Driving-distance service is temporarily unavailable", exception);
        }
    }
}

