package com.nikworkspace.AnyShare.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // Don't include null fields in JSON
public class SignalMessageDTO {

    private String type;        // OFFER, ANSWER, ICE_CANDIDATE, PEER_JOINED, etc.
    private String sessionId;   // Which session this message belongs to
    private String senderId;    // Who is sending this message
    private String targetId;    // Who should receive this message (null = broadcast)
    private Object payload;     // The actual data (SDP, ICE candidate, etc.)
    private String timestamp;   // When message was sent

    // Error-related fields
    private String code;        // Error code
    private String message;     // Error message
}