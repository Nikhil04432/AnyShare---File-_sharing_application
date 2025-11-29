package com.nikworkspace.AnyShare.constant;

public class Constant {
    private Constant(){ 

    }
    public static final String ROOM_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    public static final int ROOM_CODE_LENGTH = 6;
    public static final int SESSION_EXPIRATION_MINUTES = 5;
    public static final int MAX_PEERS_PER_SESSION = 2;
    public static final String WEBSOCKET_URL = "ws://localhost:8080/signal";
    public static final String V_1_SESSIONS = "/api/v1/sessions";
}
