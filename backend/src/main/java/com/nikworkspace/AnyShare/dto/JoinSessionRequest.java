package com.nikworkspace.AnyShare.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinSessionRequest {
    private String deviceType;  // MOBILE, DESKTOP, TABLET
    private String userAgent;   // Browser info
}