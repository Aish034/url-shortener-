package com.schwab.urlshortener.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Core entity mapping a generated short code to its original long URL.
 *
 * AI-assist note: initial field set was AI-suggested; expiresAt and
 * clickCount were added manually after reviewing the assignment's
 * "analytics" and "reliability" requirements (see AI_EXECUTION_LOG.md,
 * Task 2).
 */
@Entity
@Table(name = "short_urls", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
})
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Column(nullable = false)
    private long clickCount = 0L;

    protected ShortUrl() {
        // JPA
    }

    public ShortUrl(String shortCode, String originalUrl, Instant createdAt, Instant expiresAt) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getClickCount() {
        return clickCount;
    }

    /**
     * Assigns the generated short code after the entity's ID is known.
     * Only permitted once, from the initial "PENDING" placeholder, to
     * prevent a mapping's code from being silently changed post-creation.
     */
    public void assignShortCode(String shortCode) {
        if (!"PENDING".equals(this.shortCode)) {
            throw new IllegalStateException("Short code already assigned for id=" + id);
        }
        this.shortCode = shortCode;
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
