package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.CreateUrlResponse;
import com.schwab.urlshortener.exception.ShortUrlExpiredException;
import com.schwab.urlshortener.exception.ShortUrlNotFoundException;
import com.schwab.urlshortener.model.ShortUrl;
import com.schwab.urlshortener.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private final ShortUrlRepository repository;
    private final String baseUrl;

    public UrlShortenerService(ShortUrlRepository repository,
                                @Value("${app.base-url}") String baseUrl) {
        this.repository = repository;
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a short URL. Idempotent for repeat submissions of the same
     * originalUrl (returns the existing mapping instead of creating a
     * duplicate) — this was a manual addition after AI's first draft did not
     * de-duplicate at all, which would have let the table grow unbounded
     * with identical mappings under repeated client retries.
     */
    @Transactional
    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
        Optional<ShortUrl> existing = repository.findByOriginalUrl(request.getOriginalUrl());
        if (existing.isPresent() && !existing.get().isExpired()) {
            return toResponse(existing.get());
        }

        Instant now = Instant.now();
        Instant expiresAt = request.getTtlMinutes() != null
                ? now.plus(request.getTtlMinutes(), ChronoUnit.MINUTES)
                : null;

        // Persist first (no code yet) to obtain the auto-generated ID,
        // then derive the short code from that ID and persist again.
        ShortUrl entity = new ShortUrl("PENDING", request.getOriginalUrl(), now, expiresAt);
        entity = repository.save(entity);

        String shortCode = Base62Encoder.encode(entity.getId());
        entity.assignShortCode(shortCode);
        entity = repository.save(entity);

        return toResponse(entity);
    }

    /**
     * Resolves a short code to its original URL, throwing if missing/expired.
     * Cached because redirects are the hottest path in a URL shortener and
     * the mapping is immutable once created (aside from click count, which
     * is tracked separately via ClickEvent to avoid invalidating the cache
     * on every single click).
     */
    @Cacheable(value = "shortUrlCache", key = "#shortCode")
    @Transactional(readOnly = true)
    public ShortUrl resolve(String shortCode) {
        ShortUrl shortUrl = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));
        if (shortUrl.isExpired()) {
            throw new ShortUrlExpiredException(shortCode);
        }
        return shortUrl;
    }

    @CacheEvict(value = "shortUrlCache", key = "#shortCode")
    public void evictFromCache(String shortCode) {
        // exposed for completeness / manual invalidation (e.g. future delete API)
    }

    private CreateUrlResponse toResponse(ShortUrl entity) {
        return new CreateUrlResponse(
                entity.getShortCode(),
                baseUrl + "/" + entity.getShortCode(),
                entity.getOriginalUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}
