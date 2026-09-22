package com.schwab.urlshortener.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Records a single redirect event for analytics.
 *
 * AI-assist note (ambiguous scenario): the assignment says "analytics"
 * with no definition of what counts as a trackable event. Decision made
 * here (documented in SCENARIOS.md): log every redirect hit (not
 * deduplicated to unique visitors) plus referrer, since that is the
 * simplest defensible interpretation and unique-visitor tracking would
 * require client identity we don't have (no auth, no cookies) without
 * adding scope the assignment didn't ask for.
 */
@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_short_code", columnList = "shortCode")
})
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String shortCode;

    @Column(nullable = false)
    private Instant clickedAt;

    @Column(length = 512)
    private String referrer;

    protected ClickEvent() {
        // JPA
    }

    public ClickEvent(String shortCode, Instant clickedAt, String referrer) {
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.referrer = referrer;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public String getReferrer() {
        return referrer;
    }
}
