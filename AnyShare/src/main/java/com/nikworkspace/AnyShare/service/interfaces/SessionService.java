package com.nikworkspace.AnyShare.service.interfaces;

import com.nikworkspace.AnyShare.pojo.*;

public interface SessionService {
    SessionCreateResponse createSession(SessionCreateRequest createRequest);

     SessionInfoResponse getSessionInfo(String roomCode);

     SessionJoinResponse joinSession(String roomCode, JoinSessionRequest request);

    void closeSession(String sessionId, String token);
}
