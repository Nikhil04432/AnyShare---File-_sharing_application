package com.nikworkspace.AnyShare.service;

import com.nikworkspace.AnyShare.dto.AuthResponse;
import com.nikworkspace.AnyShare.dto.LoginRequest;
import com.nikworkspace.AnyShare.dto.RefreshTokenRequest;
import com.nikworkspace.AnyShare.dto.RegisterRequest;
import com.nikworkspace.AnyShare.entity.User;
import com.nikworkspace.AnyShare.exception.InvalidTokenException;
import com.nikworkspace.AnyShare.repository.UserRepository;
import com.nikworkspace.AnyShare.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .active(true)
                .build();

        user = userRepository.save(user);

        log.info("User registered successfully: {}", user.getEmail());

        // Generate tokens
        String token = jwtUtil.generateAuthToken(user.getEmail(), user.getId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId().toString());

        return buildAuthResponse(user, token, refreshToken);
    }

    /**
     * Login user
     */
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getEmail());

        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If authentication successful, load user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }

        log.info("User logged in successfully: {}", user.getEmail());

        // Generate tokens
        String token = jwtUtil.generateAuthToken(user.getEmail(), user.getId().toString());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId().toString());

        return buildAuthResponse(user, token, refreshToken);
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Token refresh attempt");

        try {
            // Validate refresh token
            Claims claims = jwtUtil.validateToken(request.getRefreshToken());

            // Check if it's actually a refresh token
            String tokenType = jwtUtil.getTokenType(request.getRefreshToken());
            if (!"REFRESH".equals(tokenType)) {
                throw new InvalidTokenException("Invalid token type. Expected REFRESH token");
            }

            String email = claims.getSubject();
            String userId = claims.get("userId", String.class);

            // Load user to ensure they still exist and are active
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidTokenException("User not found"));

            if (!user.getActive()) {
                throw new InvalidTokenException("Account is deactivated");
            }

            log.info("Token refreshed successfully for user: {}", email);

            // Generate new access token
            String newToken = jwtUtil.generateAuthToken(email, userId);

            // Return new token with same refresh token
            return buildAuthResponse(user, newToken, request.getRefreshToken());

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired refresh token");
        }
    }

    /**
     * Build AuthResponse from user and tokens
     */
    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(300L) // 5 minutes in seconds (from config)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }
}