package com.poly.models.services.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.auth.refresh.max-age:604800}")
    private long maxAge;

    public long getMaxAge() { return maxAge; }

    public String issue(String username, boolean rememberMe) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(key(token), (rememberMe ? "1\n" : "0\n") + username, Duration.ofSeconds(maxAge));
        return token;
    }

    public record Session(String username, boolean rememberMe) {}

    public Session consume(String token) {
        if (token == null || token.length() != 43) throw unauthorized();
        // Atomic removal ensures a refresh token can only be used once.
        String username = redis.opsForValue().getAndDelete(key(token));
        if (username == null) throw unauthorized();
        if (username.startsWith("1\n") || username.startsWith("0\n")) {
            return new Session(username.substring(2), username.startsWith("1\n"));
        }
        // Tokens issued before remember-me support were persistent.
        return new Session(username, true);
    }

    public void revoke(String token) {
        if (token != null && token.length() == 43) redis.delete(key(token));
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }

    private String key(String token) {
        try {
            return "auth:refresh:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
