package com.poly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.osrm")
public record OsrmProperties(String baseUrl) {
}
