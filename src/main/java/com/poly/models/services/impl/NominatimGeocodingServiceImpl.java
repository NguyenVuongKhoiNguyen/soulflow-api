package com.poly.models.services.impl;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.poly.config.NominatimProperties;
import com.poly.models.services.GeocodingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NominatimGeocodingServiceImpl implements GeocodingService {

    private final NominatimProperties nominatimProperties;

    @Override
    @Cacheable(value = "geocodedAddresses", key = "#address")
    public Coordinate geocodeVietnameseAddress(String address) {
        if (!StringUtils.hasText(address)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An address is required");
        }

        try {
            for (String query : buildSearchQueries(address)) {
                Coordinate coordinate = requestCoordinate(query);
                if (coordinate != null) {
                    return coordinate;
                }
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Could not find coordinates for the selected address");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Hosted Nominatim request failed: {}", exception.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Address lookup is temporarily unavailable", exception);
        }
    }

    private Coordinate requestCoordinate(String query) {
        URI uri = UriComponentsBuilder.fromHttpUrl(nominatimProperties.baseUrl())
            .path("/search")
            .queryParam("q", query)
            .queryParam("format", "jsonv2")
            .queryParam("limit", 1)
            .queryParam("countrycodes", "vn")
            .build()
            .encode()
            .toUri();

        JsonNode response = RestClient.create()
            .get()
            .uri(uri)
            .header(HttpHeaders.USER_AGENT, "SoulFlow/1.0 (shipping address lookup)")
            .retrieve()
            .body(JsonNode.class);

        JsonNode result = response == null ? null : response.path(0);
        if (result == null || result.isMissingNode()
                || !result.hasNonNull("lat") || !result.hasNonNull("lon")) {
            return null;
        }
        return new Coordinate(result.path("lat").asDouble(), result.path("lon").asDouble());
    }

    private Set<String> buildSearchQueries(String address) {
        Set<String> queries = new LinkedHashSet<>();
        String trimmedAddress = address.trim();
        queries.add(trimmedAddress);

        String normalizedAddress = trimmedAddress
            .replaceAll("(?i)\\bStreet\\b", "")
            .replaceAll("(?i)\\bWard\\b", "")
            .replaceAll("(?i)\\bDistrict\\b", "")
            .replaceAll("(?i)\\bProvince\\b", "")
            .replaceAll("\\s*,\\s*", ", ")
            .replaceAll("\\s+", " ")
            .trim();
        queries.add(normalizedAddress);
        queries.add(normalizedAddress.replaceFirst("^\\d+", "1"));

        int finalComma = trimmedAddress.lastIndexOf(',');
        if (finalComma >= 0) {
            queries.add(trimmedAddress.substring(finalComma + 1).trim());
        }
        return queries;
    }
}
