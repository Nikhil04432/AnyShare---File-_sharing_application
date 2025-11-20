package com.nikworkspace.AnyShare.util;

import com.nikworkspace.AnyShare.config.JwtConfig;
import com.nikworkspace.AnyShare.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    /**
     * Generate JWT token for a peer in a session
     *
     * @param peerId Unique identifier for the peer
     * @param sessionId Session the peer belongs to
     * @param role Role of peer (SENDER or RECEIVER)
     * @return Generated JWT token string
     */
    public String generateToken(String peerId, String sessionId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sessionId", sessionId);
        claims.put("role", role);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        String token = Jwts.builder()
                .claims(claims)                          // Custom claims
                .subject(peerId)                         // Who this token is for
                .issuedAt(now)                          // When token was created
                .expiration(expiryDate)                 // When token expires
                .signWith(getSigningKey())              // Sign with secret key
                .compact();                              // Build final token string

        log.debug("Generated JWT token for peerId: {}, sessionId: {}, expires at: {}",
                peerId, sessionId, expiryDate);

        return token;
    }

    /**
     * Validate JWT token and extract claims
     *
     * @param token JWT token to validate
     * @return Claims object containing token data
     * @throws InvalidTokenException if token is invalid, expired, or tampered
     */
    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())        // Verify signature
                    .build()
                    .parseSignedClaims(token)           // Parse and validate
                    .getPayload();                      // Extract claims

            log.debug("Token validated successfully for peerId: {}", claims.getSubject());

            return claims;

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired token: " + e.getMessage());
        }
    }

    /**
     * Extract peerId from token
     *
     * @param token JWT token
     * @return Peer ID (subject)
     */
    public String extractPeerId(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extract sessionId from token
     *
     * @param token JWT token
     * @return Session ID
     */
    public String extractSessionId(String token) {
        Claims claims = validateToken(token);
        return claims.get("sessionId", String.class);
    }

    /**
     * Extract role from token
     *
     * @param token JWT token
     * @return Role (SENDER or RECEIVER)
     */
    public String extractRole(String token) {
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Check if token is expired
     *
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (InvalidTokenException e) {
            return true; // If validation fails, consider it expired
        }
    }

    /**
     * Get signing key from secret
     * Converts secret string to SecretKey object for HMAC-SHA256
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}