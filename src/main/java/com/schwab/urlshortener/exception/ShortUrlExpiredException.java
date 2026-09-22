package com.schwab.urlshortener.exception;

public class ShortUrlExpiredException extends RuntimeException {
    public ShortUrlExpiredException(String shortCode) {
        super("Short code has expired: " + shortCode);
    }
}
