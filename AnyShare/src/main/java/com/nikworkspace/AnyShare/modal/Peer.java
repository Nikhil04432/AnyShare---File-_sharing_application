package com.nikworkspace.AnyShare.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Peer {

    private String peerId;              // Unique identifier for this peer
    private String sessionId;           // Which session this peer belongs to
    private String deviceType;          // MOBILE, DESKTOP, TABLET
    private String userAgent;           // Browser info
    private LocalDateTime joinedAt;     // When peer joined

    // WebSocket session will be added later when we implement WebSocket
    // private WebSocketSession wsSession;
}