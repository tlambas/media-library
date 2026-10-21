package com.example.medialibrary.exception;

public class MediaItemNotFoundException extends RuntimeException {
    public MediaItemNotFoundException() {}
    public MediaItemNotFoundException(String message) {
        super(message);
    }
}
