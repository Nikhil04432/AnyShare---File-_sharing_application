package com.nikworkspace.AnyShare.service;

import com.nikworkspace.AnyShare.dto.JoinSessionRequest;
import com.nikworkspace.AnyShare.dto.SessionCreateResponse;
import com.nikworkspace.AnyShare.dto.SessionJoinResponse;
import com.nikworkspace.AnyShare.dto.SignalMessageDTO;
import com.nikworkspace.AnyShare.entity.SessionEntity;
import com.nikworkspace.AnyShare.enums.SessionStatus;
import com.nikworkspace.AnyShare.exception.SessionExpiredException;
import com.nikworkspace.AnyShare.exception.SessionFullException;
import com.nikworkspace.AnyShare.model.Peer;
import com.nikworkspace.AnyShare.model.Session;
import com.nikworkspace.AnyShare.repository.SessionRepository;
import com.nikworkspace.AnyShare.repository.UserRepository;
import com.nikworkspace.AnyShare.service.impl.SessionServiceImpl;
import com.nikworkspace.AnyShare.util.CodeGenerator;
import com.nikworkspace.AnyShare.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private SessionServiceImpl service;
    // =======================
    // CREATE SESSION TEST
    // =======================

    @Test
    void createSession_shouldStoreSessionAndReturnResponse() {

        when(codeGenerator.generateRoomCode()).thenReturn("TEST123");

        // SESSION ENTITY CAPTURE
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SignalMessageDTO.SessionCreateRequest req = new SignalMessageDTO.SessionCreateRequest("DESKTOP", "Chrome");

        SessionCreateResponse response = service.createSession(req);

        // ✅ Validate output
        assertNotNull(response.getSessionId());
        assertEquals("TEST123", response.getRoomCode());
        assertNotNull(response.getQrCode());
        assertNotNull(response.getExpiresAt());

        // ✅ Ensure DB save was called
        verify(sessionRepository, times(1)).save(any(SessionEntity.class));

        // ✅ Validate cache population
        Session s = service.getOrLoadSession(response.getSessionId());
        assertEquals("TEST123", s.getRoomCode());
    }

    // =======================
    // LOAD SESSION FROM DB
    // =======================

    @Test
    void getOrLoadSession_shouldLoadFromDatabaseIfNotInMemory() {

        UUID id = UUID.randomUUID();

        SessionEntity entity = SessionEntity.builder()
                .id(id)
                .roomCode("ROOM1")
                .status(SessionStatus.WAITING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .maxPeers(2)
                .build();

        when(sessionRepository.findById(id)).thenReturn(Optional.of(entity));

        Session s = service.getOrLoadSession(id.toString());

        assertEquals("ROOM1", s.getRoomCode());
        assertEquals(SessionStatus.WAITING, s.getStatus());

        // ✅ Session now cached
        Session cached = service.getOrLoadSession(id.toString());
        assertSame(s, cached);
    }

    // =======================
    // JOIN SESSION SUCCESS
    // =======================

    @Test
    void joinSession_shouldGenerateTokenAndRegisterPeer() {

        UUID id = UUID.randomUUID();
        String room = "ROOMA";

        SessionEntity entity = SessionEntity.builder()
                .id(id)
                .roomCode(room)
                .status(SessionStatus.WAITING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .maxPeers(2)
                .build();

        when(sessionRepository.findByRoomCode(room)).thenReturn(Optional.of(entity));
        when(sessionRepository.findById(id)).thenReturn(Optional.of(entity));
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("FAKE_TOKEN");

        JoinSessionRequest req = new JoinSessionRequest("MOBILE", "Chrome");

        SessionJoinResponse response = service.joinSession(room, req);

        assertEquals("FAKE_TOKEN", response.getToken());
        assertEquals(id.toString(), response.getSessionId());
    }

    // =======================
    // SESSION FULL
    // =======================

    @Test
    void joinSession_shouldThrowIfSessionFull() {

        UUID id = UUID.randomUUID();
        String room = "FULLROOM";

        // Entity for DB fallback
        SessionEntity entity = SessionEntity.builder()
                .id(id)
                .roomCode(room)
                .status(SessionStatus.WAITING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .maxPeers(1)
                .build();

        when(sessionRepository.findById(id)).thenReturn(Optional.of(entity));

        // LOAD VIA SERVICE
        Session session = service.getOrLoadSession(id.toString());

        // SAFETY OVERRIDES
        session.setMaxPeers(1);
        session.setStatus(SessionStatus.WAITING);

        // ADD REAL CONNECTED PEER
        Peer connectedPeer = Peer.builder()
                .peerId("peer1")
                .sessionId(id.toString())
                .build();

        // THIS is what makes Peer::isConnected return true
        WebSocketSession wsSession = mock(WebSocketSession.class);
        when(wsSession.isOpen()).thenReturn(true);
        connectedPeer.setWsSession(wsSession);


        session.getPeers().clear();
        session.getPeers().put("peer1", connectedPeer);

        // PROVE TEST STATE BEFORE CALL
        long connected = session.getPeers().values().stream()
                .filter(Peer::isConnected)
                .count();

        System.err.println("TEST DEBUG > connectedPeers = " + connected);
        System.err.println("TEST DEBUG > maxPeers = " + session.getMaxPeers());

        // HARD FAIL IF WRONG
        assertEquals(1, connected);
        assertEquals(1, session.getMaxPeers());

        // ACT
        assertThrows(SessionFullException.class, () ->
                service.joinSession(room, new JoinSessionRequest("DESKTOP", "Chrome"))
        );
    }



    // =======================
    // EXPIRED SESSION
    // =======================

    @Test
    void getSessionInfo_shouldThrowIfExpired() {

        UUID id = UUID.randomUUID();
        String room = "OLD";

        SessionEntity entity = SessionEntity.builder()
                .id(id)
                .roomCode(room)
                .status(SessionStatus.WAITING)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .maxPeers(2)
                .build();

        when(sessionRepository.findByRoomCode(room)).thenReturn(Optional.of(entity));
        when(sessionRepository.findById(id)).thenReturn(Optional.of(entity));

        assertThrows(SessionExpiredException.class, () -> service.getSessionInfo(room));
    }

    // =======================
    // CLOSE SESSION
    // =======================

    @Test
    void closeSession_shouldValidateTokenAndUpdateStatus() {

        UUID id = UUID.randomUUID();
        String sid = id.toString();

        SessionEntity entity = SessionEntity.builder()
                .id(id)
                .roomCode("CL")
                .status(SessionStatus.WAITING)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .maxPeers(2)
                .build();


        when(sessionRepository.findById(id)).thenReturn(Optional.of(entity));
        when(jwtUtil.validateToken(any())).thenReturn(null);

        service.closeSession(sid, "TOKEN");

        verify(jwtUtil).validateToken("TOKEN");
        verify(sessionRepository, times(1)).save(any());
    }
}
