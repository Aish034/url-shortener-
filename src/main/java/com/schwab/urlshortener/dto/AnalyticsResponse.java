package com.schwab.urlshortener.dto;

import java.time.Instant;
import java.util.List;

public class AnalyticsResponse {
    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    private List<Instant> recentClicks;

    public AnalyticsResponse(String shortCode, String originalUrl, long totalClicks, List<Instant> recentClicks) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.totalClicks = totalClicks;
        this.recentClicks = recentClicks;
    }

    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public long getTotalClicks() { return totalClicks; }
    public List<Instant> getRecentClicks() { return recentClicks; }
}
