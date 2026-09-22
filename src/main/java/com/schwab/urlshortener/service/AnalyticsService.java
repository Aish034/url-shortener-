package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.AnalyticsResponse;
import com.schwab.urlshortener.exception.ShortUrlNotFoundException;
import com.schwab.urlshortener.model.ClickEvent;
import com.schwab.urlshortener.model.ShortUrl;
import com.schwab.urlshortener.repository.ClickEventRepository;
import com.schwab.urlshortener.repository.ShortUrlRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final ShortUrlRepository shortUrlRepository;

    public AnalyticsService(ClickEventRepository clickEventRepository,
                             ShortUrlRepository shortUrlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.shortUrlRepository = shortUrlRepository;
    }

    /**
     * Logged asynchronously so a slow analytics write never adds latency to
     * the redirect path itself (redirects are the user-facing hot path;
     * analytics is a side effect). This was an AI suggestion accepted as-is
     * after confirming Spring's default async executor is adequate for this
     * prototype's scale (would move to a message queue for production).
     */
    @Async
    public void recordClick(String shortCode, String referrer) {
        clickEventRepository.save(new ClickEvent(shortCode, Instant.now(), referrer));
    }

    public AnalyticsResponse getAnalytics(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        long totalClicks = clickEventRepository.countByShortCode(shortCode);
        List<Instant> recent = clickEventRepository.findByShortCodeOrderByClickedAtDesc(shortCode)
                .stream()
                .limit(20)
                .map(ClickEvent::getClickedAt)
                .collect(Collectors.toList());

        return new AnalyticsResponse(shortCode, shortUrl.getOriginalUrl(), totalClicks, recent);
    }
}
