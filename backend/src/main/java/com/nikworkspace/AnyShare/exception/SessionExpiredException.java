package com.nikworkspace.AnyShare.exception;

public class SessionExpiredException extends RuntimeException{
    public SessionExpiredException(String message) {
        super(message);
    }
}
