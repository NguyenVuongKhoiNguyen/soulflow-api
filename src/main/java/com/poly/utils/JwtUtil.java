package com.poly.utils;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey; 

@Component
public class JwtUtil {

    private final String SECRET = "blackfloydcantbreathebecausea12kneesonhisneckforusingacounterfeitmoneytobuyabanana"; // ≥32 chars
    private final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    
    public String generateToken(String username, List<String> roleCodes) {
        String primaryRole = roleCodes.contains("ADMIN")
                ? "ADMIN"
                : roleCodes.stream().findFirst().orElse("USER");
        return Jwts.builder()
                .setSubject(username)
                .claim("roleCode", primaryRole)
                .claim("roleCodes", roleCodes)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 8 * 60 * 60 * 1000)) //8 hours
                //.setExpiration(new Date(System.currentTimeMillis() + 10 * 1000)) //30 seconds
                .signWith(KEY)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roleCodes = extractAllClaims(token).get("roleCodes");
        if (roleCodes instanceof List<?> roles) {
            return roles.stream().map(String::valueOf).toList();
        }
        String roleCode = extractAllClaims(token).get("roleCode", String.class);
        return roleCode == null ? List.of() : List.of(roleCode);
    }

    public boolean isValid(String token) {
        try {
            extractAllClaims(token); // will throw if invalid/expired
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
