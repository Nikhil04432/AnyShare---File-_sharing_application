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

    // ============= USER AUTHENTICATION TOKENS =============

    /**
     * Generate JWT token for user authentication (login)
     *
     * @param email User's email (used as username)
     * @param userId User's UUID
     * @return Generated JWT token string
     */
    public String generateAuthToken(String email, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "AUTH"); // Mark as authentication token

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        String token = Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        log.debug("Generated auth token for email: {}, userId: {}, expires at: {}",
                email, userId, expiryDate);

        return token;
    }

    /**
     * Generate refresh token (longer expiration)
     *
     * @param email User's email
     * @param userId User's UUID
     * @return Generated refresh token
     */
    public String generateRefreshToken(String email, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "REFRESH"); // Mark as refresh token

        Date now = new Date();
        // Refresh token valid for 7 days
        Date expiryDate = new Date(now.getTime() + (7 * 24 * 60 * 60 * 1000L));

        String token = Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        log.debug("Generated refresh token for email: {}, expires at: {}", email, expiryDate);

        return token;
    }

    // ============= WEBSOCKET SESSION TOKENS (existing) =============

    /**
     * Generate JWT token for a peer in a WebSocket session
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
        claims.put("type", "WEBSOCKET"); // Mark as WebSocket token

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());

        String token = Jwts.builder()
                .claims(claims)
                .subject(peerId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        log.debug("Generated WebSocket token for peerId: {}, sessionId: {}, expires at: {}",
                peerId, sessionId, expiryDate);

        return token;
    }

    // ============= COMMON VALIDATION & EXTRACTION =============

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
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Token validated successfully for subject: {}", claims.getSubject());

            return claims;

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired token: " + e.getMessage());
        }
    }

    /**
     * Extract subject from token (email for auth, peerId for WebSocket)
     *
     * @param token JWT token
     * @return Subject
     */
    public String extractSubject(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extract peerId from WebSocket token
     *
     * @param token JWT token
     * @return Peer ID (subject)
     */
    public String extractPeerId(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    /**
     * Extract sessionId from WebSocket token
     *
     * @param token JWT token
     * @return Session ID
     */
    public String extractSessionId(String token) {
        Claims claims = validateToken(token);
        return claims.get("sessionId", String.class);
    }

    /**
     * Extract role from WebSocket token
     *
     * @param token JWT token
     * @return Role (SENDER or RECEIVER)
     */
    public String extractRole(String token) {
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }

    /**
     * Extract userId from auth token
     *
     * @param token JWT token
     * @return User ID
     */
    public String extractUserId(String token) {
        Claims claims = validateToken(token);
        return claims.get("userId", String.class);
    }

    /**
     * Get token type (AUTH, REFRESH, or WEBSOCKET)
     *
     * @param token JWT token
     * @return Token type
     */
    public String getTokenType(String token) {
        Claims claims = validateToken(token);
        return claims.get("type", String.class);
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
            return true;
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