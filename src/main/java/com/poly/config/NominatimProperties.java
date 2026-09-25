package com.poly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.nominatim")
public record NominatimProperties(String baseUrl) {
}
