package com.nikworkspace.AnyShare.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikworkspace.AnyShare.dto.SignalMessageDTO;
import com.nikworkspace.AnyShare.exception.InvalidTokenException;
import com.nikworkspace.AnyShare.exception.SessionNotFoundException;
import com.nikworkspace.AnyShare.model.Peer;
import com.nikworkspace.AnyShare.model.Session;
import com.nikworkspace.AnyShare.service.SessionStorageService;
import com.nikworkspace.AnyShare.service.impl.SessionServiceImpl;
import com.nikworkspace.AnyShare.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final SessionStorageService sessionStorage;
    private final SessionServiceImpl sessionService;

    // Constants for WebSocket attributes
    public static final String PEER_ID = "peerId";
    public static final String SESSION_ID = "sessionId";

    /**
     * Called when a new WebSocket connection is established
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        log.info("New WebSocket connection attempt - Session ID: {}", wsSession.getId());

        try {
            // Step 1: Extract token from query parameters
            String token = extractToken(wsSession);

            if (token == null) {
                log.warn("No token provided in WebSocket connection");
                sendErrorAndClose(wsSession, "MISSING_TOKEN", "Authentication token is required");
                return;
            }

            // Step 2: Validate JWT token
            Claims claims;
            try {
                claims = jwtUtil.validateToken(token);
            } catch (InvalidTokenException e) {
                log.warn("Invalid token in WebSocket connection: {}", e.getMessage());
                sendErrorAndClose(wsSession, "INVALID_TOKEN", "Invalid or expired token");
                return;
            }

            // Step 3: Extract information from token
            String peerId = claims.getSubject();
            String sessionId = claims.get(SESSION_ID, String.class);
            String role = claims.get("role", String.class);

            log.info("WebSocket authenticated - PeerId: {}, SessionId: {}, Role: {}",
                    peerId, sessionId, role);

            // Step 4: Find the session
            Session session;
            try {
                session = sessionService.getOrLoadSession(sessionId);
            } catch (SessionNotFoundException e) {
                sendErrorAndClose(wsSession, "SESSION_NOT_FOUND", e.getMessage());
                return;
            }

            // Step 5: Find the peer in session
            Peer peer = session.getPeers().get(peerId);

            if (peer == null) {
                log.warn("Peer {} not found in session {}", peerId, sessionId);
                sendErrorAndClose(wsSession, "PEER_NOT_FOUND", "Peer not found in session");
                return;
            }

            // Step 6: Attach WebSocket session to peer
            peer.setWsSession(wsSession);

            // Step 7: Store metadata in WebSocket attributes (for quick lookup later)
            wsSession.getAttributes().put(PEER_ID, peerId);
            wsSession.getAttributes().put(SESSION_ID, sessionId);
            wsSession.getAttributes().put("role", role);

            log.info("WebSocket connected successfully - PeerId: {}, SessionId: {}", peerId, sessionId);

            // Step 8: Notify other peers that this peer has joined
            notifyPeerJoined(session, peer);

        } catch (Exception e) {
            log.error("Error during WebSocket connection establishment", e);
            sendErrorAndClose(wsSession, "CONNECTION_ERROR", "Failed to establish connection");
        }
    }

    /**
     * Called when a message is received from client
     */
    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received WebSocket message: {}", payload);

        try {
            // Parse incoming message
            SignalMessageDTO signalMessage = objectMapper.readValue(payload, SignalMessageDTO.class);

            // Get sender info from WebSocket attributes
            String senderId = (String) wsSession.getAttributes().get(PEER_ID);
            String sessionId = (String) wsSession.getAttributes().get(SESSION_ID);

            // Validate message
            if (signalMessage.getType() == null) {
                sendError(wsSession, "INVALID_MESSAGE", "Message type is required");
                return;
            }

            // Set sender info (don't trust client-provided senderId)
            signalMessage.setSenderId(senderId);
            signalMessage.setSessionId(sessionId);
            signalMessage.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // Route message based on type
            switch (signalMessage.getType()) {
                case "OFFER":
                case "ANSWER":
                case "ICE_CANDIDATE":
                    handleSignalingMessage(sessionId, signalMessage);
                    break;

                default:
                    log.warn("Unknown message type: {}", signalMessage.getType());
                    sendError(wsSession, "UNKNOWN_MESSAGE_TYPE",
                            "Unknown message type: " + signalMessage.getType());
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(wsSession, "MESSAGE_PROCESSING_ERROR", "Failed to process message");
        }
    }

    /**
     * Called when WebSocket connection is closed
     */
    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
        String peerId = (String) wsSession.getAttributes().get(PEER_ID);
        String sessionId = (String) wsSession.getAttributes().get(SESSION_ID);

        log.info("WebSocket connection closed - PeerId: {}, SessionId: {}, Status: {}",
                peerId, sessionId, status);

        if (sessionId != null) {
            Session session = sessionStorage.getSessions().get(sessionId);

            if (session != null && peerId != null) {
                Peer peer = session.getPeers().get(peerId);

                if (peer != null) {
                    // Clear WebSocket session from peer
                    peer.setWsSession(null);

                    // Notify other peers
                    notifyPeerDisconnected(session, peerId, status.getReason());
                }
            }
        }
    }

    /**
     * Called when there's an error in WebSocket communication
     */
    @Override
    public void handleTransportError(WebSocketSession wsSession, Throwable exception) throws Exception {
        String peerId = (String) wsSession.getAttributes().get(PEER_ID);
        log.error("WebSocket transport error for peer: {}", peerId, exception);

        // Close connection
        if (wsSession.isOpen()) {
            wsSession.close(CloseStatus.SERVER_ERROR);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Extract JWT token from WebSocket URL query parameters
     * URL format: ws://localhost:8080/signal?token=xxx
     */
    private String extractToken(WebSocketSession wsSession) {
        String uri = wsSession.getUri().toString();

        if (uri.contains("token=")) {
            String[] parts = uri.split("token=");
            if (parts.length > 1) {
                // Handle case where there might be other query params after token
                String tokenPart = parts[1];
                int ampIndex = tokenPart.indexOf('&');
                return ampIndex > 0 ? tokenPart.substring(0, ampIndex) : tokenPart;
            }
        }

        return null;
    }

    /**
     * Handle signaling messages (OFFER, ANSWER, ICE_CANDIDATE)
     * These messages need to be forwarded to the target peer
     */
    private void handleSignalingMessage(String sessionId, SignalMessageDTO message) {
//        Session session = sessionStorage.getSessions().get(sessionId);
//
//        if (session == null) {
//            log.warn("Session not found for signaling message: {}", sessionId);
//            return;
//        }

        Session session;
        try {
            session = sessionService.getOrLoadSession(sessionId);
        } catch (SessionNotFoundException e) {
            log.warn("Session not found for signaling message: {}", sessionId);
            return;
        }

        String targetId = message.getTargetId();

        if (targetId == null) {
            log.warn("No target specified for signaling message");
            return;
        }

        Peer targetPeer = session.getPeers().get(targetId);

        if (targetPeer == null) {
            log.warn("Target peer not found: {}", targetId);
            return;
        }

        if (!targetPeer.isConnected()) {
            log.warn("Target peer {} is not connected", targetId);
            return;
        }

        // Forward message to target peer
        sendMessage(targetPeer.getWsSession(), message);

        log.debug("Forwarded {} message from {} to {}",
                message.getType(), message.getSenderId(), targetId);
    }

    /**
     * Notify all peers in session that a new peer has joined
     */
    private void notifyPeerJoined(Session session, Peer joinedPeer) {
        SignalMessageDTO notification = SignalMessageDTO.builder()
                .type("PEER_JOINED")
                .sessionId(session.getSessionId())
                .senderId(joinedPeer.getPeerId())
                .payload(joinedPeer.getDeviceType())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        // Send to all OTHER peers (not the one who just joined)
        session.getPeers().values().stream()
                .filter(peer -> !peer.getPeerId().equals(joinedPeer.getPeerId()))
                .filter(Peer::isConnected)
                .forEach(peer -> sendMessage(peer.getWsSession(), notification));

        log.info("Notified peers in session {} that peer {} joined",
                session.getSessionId(), joinedPeer.getPeerId());
    }

    /**
     * Notify all peers in session that a peer has disconnected
     */
    private void notifyPeerDisconnected(Session session, String peerId, String reason) {
        SignalMessageDTO notification = SignalMessageDTO.builder()
                .type("PEER_DISCONNECTED")
                .sessionId(session.getSessionId())
                .senderId(peerId)
                .message(reason != null ? reason : "Connection closed")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        // Send to all OTHER connected peers
        session.getPeers().values().stream()
                .filter(peer -> !peer.getPeerId().equals(peerId))
                .filter(Peer::isConnected)
                .forEach(peer -> sendMessage(peer.getWsSession(), notification));

        log.info("Notified peers in session {} that peer {} disconnected",
                session.getSessionId(), peerId);
    }

    /**
     * Send a message to a specific WebSocket session
     */
    private void sendMessage(WebSocketSession wsSession, SignalMessageDTO message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            wsSession.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send WebSocket message", e);
        }
    }

    /**
     * Send error message to WebSocket client
     */
    private void sendError(WebSocketSession wsSession, String errorCode, String errorMessage) {
        SignalMessageDTO error = SignalMessageDTO.builder()
                .type("ERROR")
                .code(errorCode)
                .message(errorMessage)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        sendMessage(wsSession, error);
    }

    /**
     * Send error and close connection
     */
    private void sendErrorAndClose(WebSocketSession wsSession, String errorCode, String errorMessage) {
        sendError(wsSession, errorCode, errorMessage);

        try {
            wsSession.close(new CloseStatus(4000, errorMessage));
        } catch (IOException e) {
            log.error("Failed to close WebSocket session", e);
        }
    }

    /**
     * Setter for sessions (will be injected from SessionService later)
     */
    public void setSessions(ConcurrentMap<String, Session> sessions) {
        this.sessionStorage.getSessions().clear();
        this.sessionStorage.getSessions().putAll(sessions);
    }
}