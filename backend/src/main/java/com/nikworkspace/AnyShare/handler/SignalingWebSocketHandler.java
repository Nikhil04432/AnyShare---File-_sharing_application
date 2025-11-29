package com.nikworkspace.AnyShare.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nikworkspace.AnyShare.dto.SignalMessageDTO;
import com.nikworkspace.AnyShare.model.Peer;
import com.nikworkspace.AnyShare.model.Session;
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
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final SessionServiceImpl sessionService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map WebSocket session ID to peer info
    private final Map<String, PeerSessionInfo> webSocketToPeer = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        log.info("WebSocket connection established: {}", wsSession.getId());

        try {
            // Extract token from query params
            String token = extractToken(wsSession);

            if (token == null) {
                log.warn("No token provided in WebSocket connection");
                wsSession.close(CloseStatus.POLICY_VIOLATION.withReason("Missing token"));
                return;
            }

            // Validate token
            Claims claims = jwtUtil.validateToken(token);
            String peerId = claims.getSubject();
            String sessionId = claims.get("sessionId", String.class);
            String tokenType = claims.get("type", String.class);

            // Verify it's a WebSocket token
            if (!"WEBSOCKET".equals(tokenType)) {
                log.warn("Invalid token type for WebSocket: {}", tokenType);
                wsSession.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid token type"));
                return;
            }

            log.info("Peer {} connecting to session {}", peerId, sessionId);

            // Get session
            Session session = sessionService.getOrLoadSession(sessionId);

            if (session == null) {
                log.warn("Session not found: {}", sessionId);
                wsSession.close(CloseStatus.POLICY_VIOLATION.withReason("Session not found"));
                return;
            }

            // CRITICAL: Check if peer already exists (reconnection scenario)
            Peer existingPeer = session.getPeers().get(peerId);
            if (existingPeer != null) {
                log.info("Peer {} is reconnecting to session {}", peerId, sessionId);

                // Update WebSocket session for reconnection
                existingPeer.setWsSession(wsSession);
            } else {
                // This shouldn't happen as peer should be added via REST API first
                log.warn("Peer {} not found in session {}. WebSocket connected before REST join?",
                        peerId, sessionId);
            }

            // Store mapping
            webSocketToPeer.put(wsSession.getId(), new PeerSessionInfo(peerId, sessionId));

            // Get the peer from session
            Peer peer = session.getPeers().get(peerId);
            if (peer != null) {
                peer.setWsSession(wsSession);

                // Notify other peers that this peer joined
                broadcastPeerJoined(session, peer);
            }

            log.info("Peer {} successfully connected to session {} via WebSocket", peerId, sessionId);

        } catch (Exception e) {
            log.error("Error during WebSocket connection: {}", e.getMessage(), e);
            wsSession.close(CloseStatus.SERVER_ERROR.withReason("Connection error"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received message from {}: {}", wsSession.getId(), payload);

        try {
            // Parse message
            SignalMessageDTO signalMessage = objectMapper.readValue(payload, SignalMessageDTO.class);

            // Get peer info
            PeerSessionInfo peerInfo = webSocketToPeer.get(wsSession.getId());
            if (peerInfo == null) {
                log.warn("Received message from unknown WebSocket session: {}", wsSession.getId());
                return;
            }

            // Get session
            Session session = sessionService.getOrLoadSession(peerInfo.sessionId);
            if (session == null) {
                log.warn("Session not found: {}", peerInfo.sessionId);
                return;
            }

            // Set sender ID
            signalMessage.setSenderId(peerInfo.peerId);
            signalMessage.setSessionId(peerInfo.sessionId);
            signalMessage.setTimestamp(LocalDateTime.now().toString());

            // Route message
            if (signalMessage.getTargetId() != null) {
                // Send to specific peer
                sendToSpecificPeer(session, signalMessage);
            } else {
                // Broadcast to all other peers
                broadcastToOthers(session, peerInfo.peerId, signalMessage);
            }

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} - Status: {}", wsSession.getId(), status);

        // Get peer info
        PeerSessionInfo peerInfo = webSocketToPeer.remove(wsSession.getId());

        if (peerInfo == null) {
            log.warn("Closed connection for unknown WebSocket session: {}", wsSession.getId());
            return;
        }

        try {
            // Get session
            Session session = sessionService.getOrLoadSession(peerInfo.sessionId);

            if (session != null) {
                // CRITICAL: Remove peer from session on disconnect
                Peer removedPeer = session.getPeers().remove(peerInfo.peerId);

                if (removedPeer != null) {
                    log.info("Peer {} removed from session {} after disconnect",
                            peerInfo.peerId, peerInfo.sessionId);

                    // Notify other peers about disconnection
                    notifyPeerDisconnected(session, peerInfo.peerId);
                } else {
                    log.warn("Peer {} not found in session {} during disconnect",
                            peerInfo.peerId, peerInfo.sessionId);
                }
            }

        } catch (Exception e) {
            log.error("Error during connection cleanup: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession wsSession, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}",
                wsSession.getId(), exception.getMessage());

        // Close connection on error
        if (wsSession.isOpen()) {
            wsSession.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * Extract token from WebSocket URL query parameters
     */
    private String extractToken(WebSocketSession wsSession) {
        try {
            URI uri = wsSession.getUri();
            if (uri == null) {
                return null;
            }

            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                return null;
            }

            // Parse query string for token parameter
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "token".equals(pair[0])) {
                    return pair[1];
                }
            }
        } catch (Exception e) {
            log.error("Error extracting token: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Broadcast that a peer has joined
     */
    private void broadcastPeerJoined(Session session, Peer joinedPeer) {
        SignalMessageDTO message = SignalMessageDTO.builder()
                .type("PEER_JOINED")
                .sessionId(session.getSessionId())
                .senderId(joinedPeer.getPeerId())
                .payload(joinedPeer.getDeviceType())
                .timestamp(LocalDateTime.now().toString())
                .build();

        broadcastToOthers(session, joinedPeer.getPeerId(), message);
    }

    /**
     * Notify peers that someone disconnected
     */
    private void notifyPeerDisconnected(Session session, String disconnectedPeerId) {
        SignalMessageDTO message = SignalMessageDTO.builder()
                .type("PEER_DISCONNECTED")
                .sessionId(session.getSessionId())
                .senderId(disconnectedPeerId)
                .timestamp(LocalDateTime.now().toString())
                .build();

        broadcastToAll(session, message);
    }

    /**
     * Send message to specific peer
     */
    private void sendToSpecificPeer(Session session, SignalMessageDTO message) {
        Peer targetPeer = session.getPeers().get(message.getTargetId());

        if (targetPeer != null && targetPeer.isConnected()) {
            sendMessage(targetPeer.getWsSession(), message);
        } else {
            log.warn("Target peer {} not found or not connected", message.getTargetId());
        }
    }

    /**
     * Broadcast to all peers except sender
     */
    private void broadcastToOthers(Session session, String senderId, SignalMessageDTO message) {
        session.getPeers().values().stream()
                .filter(peer -> !peer.getPeerId().equals(senderId))
                .filter(Peer::isConnected)
                .forEach(peer -> sendMessage(peer.getWsSession(), message));
    }

    /**
     * Broadcast to all peers including sender
     */
    private void broadcastToAll(Session session, SignalMessageDTO message) {
        session.getPeers().values().stream()
                .filter(Peer::isConnected)
                .forEach(peer -> sendMessage(peer.getWsSession(), message));
    }

    /**
     * Send message to WebSocket session
     */
    private void sendMessage(WebSocketSession wsSession, SignalMessageDTO message) {
        try {
            if (wsSession != null && wsSession.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                wsSession.sendMessage(new TextMessage(json));
                log.debug("Sent message to {}: {}", wsSession.getId(), message.getType());
            }
        } catch (IOException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    /**
     * Helper class to store peer-session mapping
     */
    private static class PeerSessionInfo {
        final String peerId;
        final String sessionId;

        PeerSessionInfo(String peerId, String sessionId) {
            this.peerId = peerId;
            this.sessionId = sessionId;
        }
    }
}