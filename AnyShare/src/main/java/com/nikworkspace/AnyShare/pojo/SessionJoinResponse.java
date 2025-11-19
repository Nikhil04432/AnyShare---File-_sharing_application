package com.nikworkspace.AnyShare.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionJoinResponse {
    private String sessionId;
    private String peerId;          // Unique ID for this peer (receiver)
    private String wsUrl;           // Where to connect WebSocket
    private String token;           // JWT for WebSocket authentication
    private String expiresAt;
}