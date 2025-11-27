package com.nikworkspace.AnyShare.service.impl;

import com.nikworkspace.AnyShare.dto.*;
import com.nikworkspace.AnyShare.entity.SessionEntity;
import com.nikworkspace.AnyShare.entity.User;
import com.nikworkspace.AnyShare.exception.*;
import com.nikworkspace.AnyShare.model.Peer;
import com.nikworkspace.AnyShare.model.Session;
import com.nikworkspace.AnyShare.enums.SessionStatus;
import com.nikworkspace.AnyShare.repository.SessionRepository;
import com.nikworkspace.AnyShare.repository.UserRepository;
import com.nikworkspace.AnyShare.service.interfaces.SessionService;
import com.nikworkspace.AnyShare.util.CodeGenerator;
import com.nikworkspace.AnyShare.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final CodeGenerator codeGenerator;
    private final JwtUtil jwtUtil;

    // Keep in-memory for active peer connections
    private final ConcurrentHashMap<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roomCodeToSessionId = new ConcurrentHashMap<>();

    private static final int SESSION_EXPIRY_MINUTES = 5;
    private static final int MAX_PEERS = 2;
    private static final String WS_URL = "ws://localhost:8080/signal";

    @Override
    @Transactional
    public SessionCreateResponse createSession(SignalMessageDTO.SessionCreateRequest request) {
        log.info("Creating new session for device: {}", request.getDeviceType());

        String sessionId = UUID.randomUUID().toString();
        String roomCode = codeGenerator.generateRoomCode();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(SESSION_EXPIRY_MINUTES);

        // Save to database
        SessionEntity entity = SessionEntity.builder()
                .id(UUID.fromString(sessionId))
                .roomCode(roomCode)
                .status(SessionStatus.WAITING)
                .maxPeers(MAX_PEERS)
                .expiresAt(expiresAt)
                .build();

        sessionRepository.save(entity);

        // Create in-memory session for WebRTC
        Session session = Session.builder()
                .sessionId(sessionId)
                .roomCode(roomCode)
                .status(SessionStatus.WAITING)
                .createdAt(now)
                .expiresAt(expiresAt)
                .maxPeers(MAX_PEERS)
                .build();

        activeSessions.put(sessionId, session);
        roomCodeToSessionId.put(roomCode, sessionId);

        log.info("Session created - ID: {}, Code: {}", sessionId, roomCode);

        String qrCodePayload = String.format(
                "{\"sessionId\":\"%s\",\"roomCode\":\"%s\",\"wsUrl\":\"%s\",\"expiresAt\":\"%s\"}",
                sessionId, roomCode, WS_URL, formatDateTime(expiresAt)
        );

        return SessionCreateResponse.builder()
                .sessionId(sessionId)
                .roomCode(roomCode)
                .qrCode(qrCodePayload)
                .wsUrl(WS_URL)
                .expiresAt(formatDateTime(expiresAt))
                .createdAt(formatDateTime(now))
                .build();
    }

    @Override
    public SessionInfoResponse getSessionInfo(String roomCode) {
        log.info("Fetching session info for roomCode: {}", roomCode);

        String sessionId = roomCodeToSessionId.get(roomCode);

        if (sessionId == null) {
            SessionEntity entity = sessionRepository.findByRoomCode(roomCode)
                    .orElseThrow(() -> new SessionNotFoundException(
                            "Session with code " + roomCode + " does not exist or has expired"
                    ));
            sessionId = entity.getId().toString();
        }

        Session session = activeSessions.get(sessionId);

        if (session == null) {
            SessionEntity entity = sessionRepository.findById(UUID.fromString(sessionId))
                    .orElseThrow(() -> new SessionNotFoundException(
                            "Session with code " + roomCode + " does not exist"
                    ));

            if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new SessionExpiredException(
                        "Session expired at " + formatDateTime(entity.getExpiresAt())
                );
            }

            session = convertToSession(entity);
            activeSessions.put(sessionId, session);
            roomCodeToSessionId.put(roomCode, sessionId);
        }

        if (session.isExpired()) {
            cleanupSession(session);
            throw new SessionExpiredException(
                    "Session expired at " + formatDateTime(session.getExpiresAt())
            );
        }

        return SessionInfoResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus().name())
                .peersConnected(session.getPeersConnected())
                .maxPeers(session.getMaxPeers())
                .canJoin(session.canJoin())
                .expiresAt(formatDateTime(session.getExpiresAt()))
                .build();
    }

    @Override
    @Transactional
    public SessionJoinResponse joinSession(String roomCode, JoinSessionRequest request) {
        log.info("Processing join request for roomCode: {}", roomCode);

        String sessionId = roomCodeToSessionId.get(roomCode);

        if (sessionId == null) {
            SessionEntity entity = sessionRepository.findByRoomCode(roomCode)
                    .orElseThrow(() -> new SessionNotFoundException(
                            "Session with code " + roomCode + " does not exist"
                    ));
            sessionId = entity.getId().toString();
        }

        Session session = activeSessions.get(sessionId);
        log.info("Found session in memory: {}", (session != null));

        if (session == null) {
            SessionEntity entity = sessionRepository.findById(UUID.fromString(sessionId))
                    .orElseThrow(() -> new SessionNotFoundException(
                            "Session with code " + roomCode + " does not exist"
                    ));

            session = convertToSession(entity);
            activeSessions.put(sessionId, session);
            roomCodeToSessionId.put(roomCode, sessionId);
        }

        if (session.isExpired()) {
            cleanupSession(session);
            throw new SessionExpiredException(
                    "Session expired at " + formatDateTime(session.getExpiresAt())
            );
        }

        if (session.getPeersConnected() >= session.getMaxPeers()) {
            throw new SessionFullException(
                    "Session is full. Maximum " + session.getMaxPeers() + " peers allowed."
            );
        }

        if (session.getStatus() != SessionStatus.WAITING) {
            throw new InvalidSessionStateException(
                    "Session not accepting connections. Status: " + session.getStatus()
            );
        }

        String peerId = "peer-" + UUID.randomUUID().toString().substring(0, 8);

        Peer peer = Peer.builder()
                .peerId(peerId)
                .sessionId(sessionId)
                .deviceType(request.getDeviceType())
                .userAgent(request.getUserAgent())
                .joinedAt(LocalDateTime.now())
                .build();

        String role = session.getPeers().isEmpty() ? "SENDER" : "RECEIVER";
        String token = jwtUtil.generateToken(peerId, sessionId, role);

        session.getPeers().put(peerId, peer);

        if (session.getPeersConnected() >= session.getMaxPeers()) {
            session.setStatus(SessionStatus.CONNECTED);

            // Update in database
            sessionRepository.findById(UUID.fromString(sessionId)).ifPresent(entity -> {
                entity.setStatus(SessionStatus.CONNECTED);
                sessionRepository.save(entity);
            });
        }

        log.info("Peer {} joined session {}", peerId, sessionId);

        return SessionJoinResponse.builder()
                .sessionId(sessionId)
                .peerId(peerId)
                .wsUrl(WS_URL)
                .token(token)
                .expiresAt(formatDateTime(session.getExpiresAt()))
                .build();
    }

    @Override
    @Transactional
    public void closeSession(String sessionId, String token) {
        log.info("Closing session: {}", sessionId);

        jwtUtil.validateToken(token);

        Session session = activeSessions.get(sessionId);

        if (session == null) {
            SessionEntity entity = sessionRepository.findById(UUID.fromString(sessionId))
                    .orElseThrow(() -> new SessionNotFoundException("Session not found"));

            session = convertToSession(entity);
        }

        session.setStatus(SessionStatus.CLOSED);

        // Update in database
        sessionRepository.findById(UUID.fromString(sessionId)).ifPresent(entity -> {
            entity.setStatus(SessionStatus.CLOSED);
            entity.setClosedAt(LocalDateTime.now());
            sessionRepository.save(entity);
        });

        cleanupSession(session);

        log.info("Session {} closed", sessionId);
    }

    private void cleanupSession(Session session) {
        activeSessions.remove(session.getSessionId());
        roomCodeToSessionId.remove(session.getRoomCode());
    }

    private Session convertToSession(SessionEntity entity) {
        return Session.builder()
                .sessionId(entity.getId().toString())
                .roomCode(entity.getRoomCode())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .maxPeers(entity.getMaxPeers())
                .build();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public Session getOrLoadSession(String sessionId) {
        // Check in memory first
        Session session = activeSessions.get(sessionId);

        if (session == null) {
            log.info("Session not found in memory — loading from database: {}", sessionId);

            SessionEntity entity = sessionRepository.findById(UUID.fromString(sessionId))
                    .orElseThrow(() -> new SessionNotFoundException("Session " + sessionId + " not found"));

            session = convertToSession(entity);
            activeSessions.put(sessionId, session);
            roomCodeToSessionId.put(entity.getRoomCode(), sessionId);
        }

        return session;
    }

}