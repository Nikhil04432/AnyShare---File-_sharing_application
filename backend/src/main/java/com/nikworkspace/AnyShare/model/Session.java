package com.nikworkspace.AnyShare.model;

import com.nikworkspace.AnyShare.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    private String sessionId;              // UUID
    private String roomCode;               // SWIFT-7284 format
    private SessionStatus status;          // WAITING, CONNECTED, EXPIRED, CLOSED
    private LocalDateTime createdAt;       // When session was created
    private LocalDateTime expiresAt;       // When session expires (5 mins from creation)
    private int maxPeers;                  // Maximum peers allowed (2 for MVP)

    // Store connected peers (peerId -> Peer object)
    // ConcurrentHashMap because multiple threads might access
    @Builder.Default
    private Map<String, com.nikworkspace.AnyShare.model.Peer> peers = new ConcurrentHashMap<>();

    /**
     * Check if session is expired based on current time
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if new peers can join this session
     */
    public boolean canJoin() {
        return status == SessionStatus.WAITING
                && ( peers == null ? 0 : peers.size()) < maxPeers
                && !isExpired();
    }

    /**
     * Get current number of connected peers
     */
    public int getPeersConnected() {
        return peers == null ? 0 : peers.size();
    }
}