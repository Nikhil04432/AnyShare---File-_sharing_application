package com.nikworkspace.AnyShare.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoResponse {
    private String sessionId;
    private String status;          // WAITING, CONNECTED, EXPIRED
    private Integer peersConnected; // How many peers already in (0, 1, 2)
    private Integer maxPeers;       // Always 2 for MVP
    private Boolean canJoin;        // Computed: is it joinable?
    private String expiresAt;
}