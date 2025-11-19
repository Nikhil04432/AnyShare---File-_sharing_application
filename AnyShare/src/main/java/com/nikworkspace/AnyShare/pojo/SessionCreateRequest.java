package com.nikworkspace.AnyShare.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateRequest {
    private String deviceType;   // e.g., MOBILE, DESKTOP, TABLET
    private String userAgent;    // Browser or app info
}
