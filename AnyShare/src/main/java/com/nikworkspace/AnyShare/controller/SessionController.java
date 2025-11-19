package com.nikworkspace.AnyShare.controller;

import com.nikworkspace.AnyShare.pojo.*;
import com.nikworkspace.AnyShare.service.interfaces.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    /**
     * Create a new file-sharing session (Sender initiates)
     *
     * @param request Device type and user agent info
     * @return Session details with QR code for sharing
     */
    @PostMapping
    public ResponseEntity<SessionCreateResponse> createSession(
            @RequestBody SessionCreateRequest request
    ) {
        log.info("Creating session - Device: {}, UA: {}",
                request.getDeviceType(), request.getUserAgent());

        SessionCreateResponse response = sessionService.createSession(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get session information (Receiver checks before joining)
     *
     * @param roomCode The human-readable room code (e.g., SWIFT-7284)
     * @return Session status and joinability info
     */
    @GetMapping("/{roomCode}")
    public ResponseEntity<SessionInfoResponse> getSessionInfo(
            @PathVariable String roomCode
    ) {
        log.info("Getting session info for roomCode={}", roomCode);

        SessionInfoResponse response = sessionService.getSessionInfo(roomCode);

        return ResponseEntity.ok(response);
    }

    /**
     * Join an existing session (Receiver joins)
     *
     * @param roomCode The room code to join
     * @param request Device type and user agent info
     * @return Peer ID, WebSocket URL, and auth token
     */
    @PostMapping("/{roomCode}/join")
    public ResponseEntity<SessionJoinResponse> joinSession(
            @PathVariable String roomCode,
            @RequestBody JoinSessionRequest request
    ) {
        log.info("Joining session - roomCode={}, Device: {}",
                roomCode, request.getDeviceType());

        SessionJoinResponse response = sessionService.joinSession(roomCode, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Close/terminate a session (Either sender or receiver can close)
     * Requires authentication token to ensure only participants can close
     *
     * @param sessionId The session UUID to close
     * @param authHeader Authorization header with Bearer token
     * @return No content on success
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> closeSession(
            @PathVariable String sessionId,
            @RequestHeader("Authorization") String authHeader
    ) {
        log.info("Close session endpoint called for sessionId={}", sessionId);

        // Extract token from "Bearer <token>" format
        String token = extractToken(authHeader);

        // Service handles validation and closure
        sessionService.closeSession(sessionId, token);

        // 204 No Content - successful deletion, no body needed
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to extract JWT token from Authorization header
     *
     * @param authHeader Full authorization header (e.g., "Bearer eyJhbGc...")
     * @return Extracted token string
     * @throws IllegalArgumentException if header format is invalid
     */
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format");
            throw new IllegalArgumentException("Authorization header must be in format: Bearer <token>");
        }
        return authHeader.substring(7); // Remove "Bearer " prefix (7 characters)
    }
}