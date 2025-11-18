package com.nikworkspace.AnyShare.service.Impl;

import com.nikworkspace.AnyShare.dto.SessionCreateRequest;
import com.nikworkspace.AnyShare.dto.SessionCreateResponse;
import com.nikworkspace.AnyShare.service.interfaces.SessionService;
import org.springframework.stereotype.Service;

@Service
public class SessionServiceImpl implements SessionService {
    public SessionCreateResponse createSession(SessionCreateRequest createRequest) {

        // TODO: Implement actual session creation logic here.


        // For now, return a dummy response.
        return new SessionCreateResponse("dummy-session-id-12345", "DUMMY-0001");
    }

    @Override
    public SessionCreateResponse getSession(String roomCode) {

        // TODO: Implement actual session retrieval logic here.

        // dummy return
        SessionCreateResponse response = new SessionCreateResponse();
        response.setSessionId("dummy-session-id-12345");
        response.setRoomCode(roomCode);


        return response;
    }
}
