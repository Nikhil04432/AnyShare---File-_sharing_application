package com.nikworkspace.AnyShare.service.impl;

import com.nikworkspace.AnyShare.constant.Constant;
import com.nikworkspace.AnyShare.dto.*;
import com.nikworkspace.AnyShare.exception.*;
import com.nikworkspace.AnyShare.model.Session;
import com.nikworkspace.AnyShare.enums.SessionStatus;
import com.nikworkspace.AnyShare.service.SessionStorageService;
import com.nikworkspace.AnyShare.service.interfaces.SessionService;
import com.nikworkspace.AnyShare.util.CodeGenerator;
import com.nikworkspace.AnyShare.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.nikworkspace.AnyShare.model.Peer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionStorageService sessionStorage;
    private final CodeGenerator codeGenerator;
    private final JwtUtil jwtUtil;

    // Configuration constants

    private static final int SESSION_EXPIRY_MINUTES = Constant.SESSION_EXPIRATION_MINUTES;
    private static final int MAX_PEERS = Constant.MAX_PEERS_PER_SESSION;
    private static final String WS_URL = Constant.WEBSOCKET_URL; // Will configure properly later

    @Override
    public SessionCreateResponse createSession(SignalMessageDTO.SessionCreateRequest request) {
        log.info("Creating new session for device: {}", request.getDeviceType());

        // Step 1: Generate unique identifiers
        String sessionId = UUID.randomUUID().toString();
        String roomCode = codeGenerator.generateRoomCode();

        // Step 2: Calculate expiry time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(SESSION_EXPIRY_MINUTES);

        // Step 3: Create Session object
        Session session = Session.builder()
                .sessionId(sessionId)
                .roomCode(roomCode)
                .status(SessionStatus.WAITING)
                .createdAt(now)
                .expiresAt(expiresAt)
                .maxPeers(MAX_PEERS)
                .build();

        // Step 4: Store in memory

        sessionStorage.getSessions().put(sessionId, session);
        sessionStorage.getRoomCodeToSessionId().put(roomCode, sessionId);

        log.info("Session created - ID: {}, Code: {}, E" +
                        "xpires: {}",
                sessionId, roomCode, expiresAt);

        // Step 5: Generate QR code payload (JSON that frontend will encode)
        String qrCodePayload = generateQrCodePayload(sessionId, roomCode, expiresAt);

        // Step 6: Build response
        return SessionCreateResponse.builder()
                .sessionId(sessionId)
                .roomCode(roomCode)
                .qrCode(qrCodePayload)
                .wsUrl(WS_URL)
                .expiresAt(formatDateTime(expiresAt))
                .createdAt(formatDateTime(now))
                .build();
    }

    /**
     * Generate JSON payload for QR code
     * Frontend will encode this into actual QR code image
     */
    private String generateQrCodePayload(String sessionId, String roomCode, LocalDateTime expiresAt) {
        // Simple JSON string (we'll use proper JSON library later if needed)
        return String.format(
                "{\"sessionId\":\"%s\",\"roomCode\":\"%s\",\"wsUrl\":\"%s\",\"expiresAt\":\"%s\"}",
                sessionId, roomCode, WS_URL, formatDateTime(expiresAt)
        );
    }

    /**
     * Format LocalDateTime to ISO-8601 string (standard format)
     * Example: "2025-11-19T15:30:00"
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public SessionInfoResponse getSessionInfo(String roomCode) {
        log.info("Fetching session info for roomCode: {}", roomCode);

        // Step 1: Find sessionId using roomCode
        String sessionId = sessionStorage.getRoomCodeToSessionId().get(roomCode);

        // Step 2: Check if room code exists
        if (sessionId == null) {
            log.warn("Room code not found: {}", roomCode);
            throw new SessionNotFoundException(
                    "Session with code " + roomCode + " does not exist or has expired"
            );
        }

        // Step 3: Get the session
        Session session = sessionStorage.getSessions().get(sessionId);

        // Step 4: Double-check session exists (defensive programming)
        if (session == null) {
            log.error("Inconsistent state: roomCode exists but session not found. SessionId: {}", sessionId);
            // Clean up the orphaned roomCode mapping
            sessionStorage.getRoomCodeToSessionId().remove(roomCode);
            throw new SessionNotFoundException(
                    "Session with code " + roomCode + " does not exist or has expired"
            );
        }

        // Step 5: Check if session is expired
        if (session.isExpired()) {
            log.info("Session {} is expired. Cleaning up...", sessionId);
            // Clean up expired session
            cleanupSession(session);
            throw new SessionExpiredException(
                    "Session with code " + roomCode + " has expired at " +
                            formatDateTime(session.getExpiresAt())
            );
        }

        // Step 6: Build response with session info
        SessionInfoResponse response = SessionInfoResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus().name())  // Convert enum to string
                .peersConnected(session.getPeersConnected())
                .maxPeers(session.getMaxPeers())
                .canJoin(session.canJoin())  // Computed field
                .expiresAt(formatDateTime(session.getExpiresAt()))
                .build();

        log.info("Session info retrieved - ID: {}, Status: {}, CanJoin: {}",
                sessionId, session.getStatus(), response.getCanJoin());

        return response;
    }

    /**
     * Clean up expired or closed session from storage
     * Removes from both maps to free memory
     */
    private void cleanupSession(Session session) {
        sessionStorage.getSessions().remove(session.getSessionId());
        sessionStorage.getRoomCodeToSessionId().remove(session.getRoomCode());
        log.info("Session cleaned up - ID: {}, Code: {}",
                session.getSessionId(), session.getRoomCode());
    }

    @Override
    public SessionJoinResponse joinSession(String roomCode, JoinSessionRequest request) {
        log.info("Processing join request for roomCode: {}, device: {}",
                roomCode, request.getDeviceType());

        // Step 1: Find sessionId using roomCode
        String sessionId = sessionStorage.getRoomCodeToSessionId().get(roomCode);

        if (sessionId == null) {
            log.warn("Room code not found: {}", roomCode);
            throw new SessionNotFoundException(
                    "Session with code " + roomCode + " does not exist or has expired"
            );
        }

        // Step 2: Get the session
        Session session = sessionStorage.getSessions().get(sessionId);

        if (session == null) {
            log.error("Inconsistent state: roomCode exists but session not found");
            sessionStorage.getRoomCodeToSessionId().remove(roomCode);
            throw new SessionNotFoundException(
                    "Session with code " + roomCode + " does not exist or has expired"
            );
        }

        // Step 3: Check if session is expired
        if (session.isExpired()) {
            log.info("Session {} is expired. Cleaning up...", sessionId);
            cleanupSession(session);
            throw new SessionExpiredException(
                    "Session with code " + roomCode + " has expired at " +
                            formatDateTime(session.getExpiresAt())
            );
        }

        // Step 4: Check if session is full
        if (session.getPeersConnected() >= session.getMaxPeers()) {
            log.warn("Session {} is full. Current peers: {}, Max: {}",
                    sessionId, session.getPeersConnected(), session.getMaxPeers());
            throw new SessionFullException(
                    "Session is already full. Maximum " + session.getMaxPeers() + " peers allowed."
            );
        }

        // Step 5: Check session status (must be WAITING)
        if (session.getStatus() != SessionStatus.WAITING) {
            log.warn("Session {} is not in WAITING state. Current status: {}",
                    sessionId, session.getStatus());
            throw new InvalidSessionStateException(
                    "Session is no longer accepting connections. Current status: " + session.getStatus()
            );
        }

        // Step 6: Create Peer object
        String peerId = "peer-" + UUID.randomUUID().toString().substring(0, 8);

        Peer peer = Peer.builder()
                .peerId(peerId)
                .sessionId(sessionId)
                .deviceType(request.getDeviceType())
                .userAgent(request.getUserAgent())
                .joinedAt(LocalDateTime.now())
                .build();

        log.info("Created peer - ID: {}, Device: {}", peerId, request.getDeviceType());

        // Step 7: Generate JWT token for this peer
        String role = determineRole(session, peerId);
        String token = jwtUtil.generateToken(peerId, sessionId, role);

        log.info("Generated JWT token for peer: {}, role: {}", peerId, role);

        // Step 8: Add peer to session (thread-safe operation)
        session.getPeers().put(peerId, peer);

        // Step 9: Update session status to CONNECTED
        if (session.getPeersConnected() >= session.getMaxPeers()) {
            session.setStatus(SessionStatus.CONNECTED);
            log.info("Session {} status changed to CONNECTED. All peers joined.", sessionId);
        }

        // Step 10: Build and return response
        SessionJoinResponse response = SessionJoinResponse.builder()
                .sessionId(sessionId)
                .peerId(peerId)
                .wsUrl(WS_URL)
                .token(token)
                .expiresAt(formatDateTime(session.getExpiresAt()))
                .build();

        log.info("Peer {} successfully joined session {}", peerId, sessionId);

        // print the data in session for debugging
        log.info(" Current session peers: {}", session.getPeers().keySet().toArray());


        return response;
    }

    /**
     * Determine role of peer in session
     * First peer = SENDER, second peer = RECEIVER
     *
     * @param session The session
     * @param peerId The peer being added
     * @return Role string (SENDER or RECEIVER)
     */
    private String determineRole(Session session, String peerId) {
        // If this is the first peer, they're the SENDER
        // If this is the second peer, they're the RECEIVER
        return session.getPeers().isEmpty() ? "SENDER" : "RECEIVER";
    }

    // ... existing methods (cleanupSession, formatDateTime) ...


    @Override
    public void closeSession(String sessionId, String token) {
        log.info("Processing close request for sessionId: {}", sessionId);

        // Step 1: Validate JWT token and extract claims
        Claims claims;
        try {
            claims = jwtUtil.validateToken(token);
        } catch (InvalidTokenException e) {
            log.warn("Invalid token provided for closing session {}: {}", sessionId, e.getMessage());
            throw new UnauthorizedException("Invalid or expired token");
        }

        // Step 2: Extract peerId from token
        String peerId = claims.getSubject();
        String tokenSessionId = claims.get("sessionId", String.class);

        log.debug("Token validated - peerId: {}, tokenSessionId: {}", peerId, tokenSessionId);

        // Step 3: Verify token's sessionId matches the requested sessionId
        if (!tokenSessionId.equals(sessionId)) {
            log.warn("Session ID mismatch - Token: {}, Requested: {}", tokenSessionId, sessionId);
            throw new UnauthorizedException(
                    "Token does not belong to this session"
            );
        }

        // Step 4: Find the session
        Session session = sessionStorage.getSessions().get(sessionId);

        if (session == null) {
            log.warn("Session not found: {}", sessionId);
            throw new SessionNotFoundException(
                    "Session with id " + sessionId + " does not exist or has already been closed"
            );
        }

        // Step 5: Verify the peer belongs to this session
        if (!session.getPeers().containsKey(peerId)) {
            log.warn("Peer {} is not part of session {}", peerId, sessionId);
            throw new UnauthorizedException(
                    "You are not authorized to close this session"
            );
        }

        // Step 6: Mark session as CLOSED (before cleanup for logging)
        session.setStatus(SessionStatus.CLOSED);

        log.info("Session {} being closed by peer {}", sessionId, peerId);

        // Step 7: Notify other peers (WebSocket notification - will implement later)
        // For now, just log
        session.getPeers().forEach((id, peer) -> {
            if (!id.equals(peerId)) {
                log.info("Would notify peer {} that session is closing", id);

                // TODO: Send WebSocket message to peer when we implement WebSocket


            }
        });

        // Step 8: Clean up the session
        cleanupSession(session);

        log.info("Session {} successfully closed by peer {}", sessionId, peerId);
    }
}
