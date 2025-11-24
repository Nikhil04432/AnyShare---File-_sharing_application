package com.nikworkspace.AnyShare.service.interfaces;

import com.nikworkspace.AnyShare.dto.*;

public interface SessionService {
    SessionCreateResponse createSession(SignalMessageDTO.SessionCreateRequest createRequest);

     SessionInfoResponse getSessionInfo(String roomCode);

     SessionJoinResponse joinSession(String roomCode, JoinSessionRequest request);

    void closeSession(String sessionId, String token);
}
