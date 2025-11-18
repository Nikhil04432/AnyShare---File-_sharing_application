package com.nikworkspace.AnyShare.dto;

import com.nikworkspace.AnyShare.service.interfaces.SessionService;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
public class SessionCreateResponse {

    private String sessionId;   // UUID
    private String roomCode;    // Human readable e.g., SWIFT-7284

    public SessionCreateResponse(String sessionId, String roomCode) {
        this.sessionId = sessionId;
        this.roomCode = roomCode;
    }

    public SessionCreateResponse() {

    }


//    private String qrCode;      // Base64 encoded JSON payload (sessionId + roomCode)
//    private String wsUrl;       // WebSocket signaling endpoint
//
//    private String expiresAt;   // ISO timestamp (UTC)
//    private String createdAt;   // ISO timestamp (UTC)
}
