package com.nikworkspace.AnyShare.enums;

public enum SessionStatus {
    WAITING,      // Session created, waiting for receiver to join
    CONNECTED,    // Receiver has joined, peers can transfer
    EXPIRED,      // Session time limit exceeded
    CLOSED        // Session manually closed by user
}