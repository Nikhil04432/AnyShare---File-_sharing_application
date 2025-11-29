package com.nikworkspace.AnyShare.service;

import com.nikworkspace.AnyShare.model.Session;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shared storage for sessions
 * Used by both REST controllers and WebSocket handlers
 */
@Service
public class SessionStorageService {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> roomCodeToSessionId = new ConcurrentHashMap<>();

    public ConcurrentMap<String, Session> getSessions() {
        return sessions;
    }

    public ConcurrentMap<String, String> getRoomCodeToSessionId() {
        return roomCodeToSessionId;
    }
}