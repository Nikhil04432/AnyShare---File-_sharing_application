package com.nikworkspace.AnyShare.service.interfaces;

import com.nikworkspace.AnyShare.dto.SessionCreateRequest;
import com.nikworkspace.AnyShare.dto.SessionCreateResponse;

public interface SessionService {
    public SessionCreateResponse createSession(SessionCreateRequest createRequest);

    public SessionCreateResponse getSession(String roomCode);
}
