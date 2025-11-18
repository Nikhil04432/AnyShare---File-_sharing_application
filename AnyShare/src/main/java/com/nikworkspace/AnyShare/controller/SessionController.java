package com.nikworkspace.AnyShare.controller;

import com.nikworkspace.AnyShare.dto.SessionCreateRequest;
import com.nikworkspace.AnyShare.dto.SessionCreateResponse;
import com.nikworkspace.AnyShare.service.Impl.SessionServiceImpl;

import com.nikworkspace.AnyShare.service.interfaces.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SessionController {

    private final SessionService sessionService;

    /**
     * Create a new file-sharing session.
     * (Sender initiates)
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionCreateResponse> createSession(
            @RequestBody SessionCreateRequest request
    ) {
        log.info("Create session endpoint called Device: {}, UA: {}",
                request.getDeviceType(), request.getUserAgent());

        SessionCreateResponse response = sessionService.createSession(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Simple endpoint for testing purposes.
     */
    @GetMapping("/sessions{roomCode}")
    public SessionCreateResponse getSessionInfo(@PathVariable String roomCode) {
        log.info("Get session info endpoint called.");

        SessionCreateResponse sessionResponse = sessionService.getSession(roomCode);


        return sessionResponse;
    }
}
