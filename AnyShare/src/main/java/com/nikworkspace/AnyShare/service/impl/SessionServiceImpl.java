package com.nikworkspace.AnyShare.service.impl;

import com.nikworkspace.AnyShare.exception.SessionExpiredException;
import com.nikworkspace.AnyShare.exception.SessionNotFoundException;
import com.nikworkspace.AnyShare.pojo.*;
import com.nikworkspace.AnyShare.modal.Session;
import com.nikworkspace.AnyShare.enums.SessionStatus;
import com.nikworkspace.AnyShare.service.interfaces.SessionService;
import com.nikworkspace.AnyShare.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    // In-memory storage for MVP (will use Redis later for production)
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    // Quick lookup: roomCode -> sessionId
    private final ConcurrentHashMap<String, String> roomCodeToSessionId = new ConcurrentHashMap<>();

    private final CodeGenerator codeGenerator;

    // Configuration constants
    private static final int SESSION_EXPIRY_MINUTES = 5;
    private static final int MAX_PEERS = 2;
    private static final String WS_URL = "ws://localhost:8080/signal"; // Will configure properly later

    @Override
    public SessionCreateResponse createSession(SessionCreateRequest request) {
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
        sessions.put(sessionId, session);
        roomCodeToSessionId.put(roomCode, sessionId);

        log.info("Session created - ID: {}, Code: {}, Expires: {}",
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
        String sessionId = roomCodeToSessionId.get(roomCode);

        // Step 2: Check if room code exists
        if (sessionId == null) {
            log.warn("Room code not found: {}", roomCode);
            throw new SessionNotFoundException(
                    "Session with code " + roomCode + " does not exist or has expired"
            );
        }

        // Step 3: Get the session
        Session session = sessions.get(sessionId);

        // Step 4: Double-check session exists (defensive programming)
        if (session == null) {
            log.error("Inconsistent state: roomCode exists but session not found. SessionId: {}", sessionId);
            // Clean up the orphaned roomCode mapping
            roomCodeToSessionId.remove(roomCode);
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
        sessions.remove(session.getSessionId());
        roomCodeToSessionId.remove(session.getRoomCode());
        log.info("Session cleaned up - ID: {}, Code: {}",
                session.getSessionId(), session.getRoomCode());
    }

    @Override
    public SessionJoinResponse joinSession(String roomCode, JoinSessionRequest request) {
        log.info("Joining session for roomCode={}", roomCode);

        // TODO: Implement actual session joining logic here.


        // return dummy response for now
//        SessionJoinResponse sessionJoinResponse = new SessionJoinResponse();
//        sessionJoinResponse.setPeerId("dummy-peer-id-67890");
//        sessionJoinResponse.setWsUrl("wss://dummy-websocket-url");
//        sessionJoinResponse.setToken("dummy-jwt-token");
//        sessionJoinResponse.setSessionId("dummy-session-id-12345");
//        sessionJoinResponse.setExpiresAt("2024-12-31T23:59:59Z");
//
//        log.info(" Joined session successfully for PeerID: {}", sessionJoinResponse.getPeerId());

        return null;
    }

    @Override
    public void closeSession(String sessionId, String token) {

        // TODO: Implement actual session closing logic here.


        log.info(" Session with ID: {} closed successfully.", sessionId);
    }
}
