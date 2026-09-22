package com.schwab.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class CreateUrlRequest {

    @NotBlank(message = "originalUrl must not be blank")
    @Pattern(regexp = "^(https?)://.+", message = "originalUrl must start with http:// or https://")
    private String originalUrl;

    /** Optional: minutes until the short link expires. Null = never expires. */
    @Positive(message = "ttlMinutes must be positive if provided")
    private Long ttlMinutes;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Long getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(Long ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }
}
