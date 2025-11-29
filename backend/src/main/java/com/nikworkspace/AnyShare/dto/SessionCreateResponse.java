package com.nikworkspace.AnyShare.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateResponse {

    private String sessionId;   // UUID
    private String roomCode;    // Human readable e.g., SWIFT-7284

    private String qrCode;      // Base64 encoded JSON payload (sessionId + roomCode)
    private String wsUrl;       // WebSocket signaling endpoint

    private String expiresAt;   // ISO timestamp (UTC)
    private String createdAt;   // ISO timestamp (UTC)

}
