package com.nikworkspace.AnyShare.controller;

import com.nikworkspace.AnyShare.dto.AuthResponse;
import com.nikworkspace.AnyShare.dto.LoginRequest;
import com.nikworkspace.AnyShare.dto.RefreshTokenRequest;
import com.nikworkspace.AnyShare.dto.RegisterRequest;
import com.nikworkspace.AnyShare.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register - email: {}", request.getEmail());

        AuthResponse response = authService.register(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Login user
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login - email: {}", request.getEmail());

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/v1/auth/refresh");

        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user info (requires authentication)
     * GET /api/v1/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<String> getCurrentUser() {
        log.info("GET /api/v1/auth/me");

        // This endpoint is protected, so only authenticated users can access
        // Spring Security automatically populates SecurityContext
        return ResponseEntity.ok("Authenticated user endpoint - implement user details here");
    }
}