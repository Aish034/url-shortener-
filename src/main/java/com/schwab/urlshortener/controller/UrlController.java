package com.schwab.urlshortener.controller;

import com.schwab.urlshortener.config.TokenBucketRateLimiter;
import com.schwab.urlshortener.dto.AnalyticsResponse;
import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.CreateUrlResponse;
import com.schwab.urlshortener.exception.RateLimitExceededException;
import com.schwab.urlshortener.model.ShortUrl;
import com.schwab.urlshortener.service.AnalyticsService;
import com.schwab.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;
    private final TokenBucketRateLimiter rateLimiter;

    public UrlController(UrlShortenerService urlShortenerService,
                          AnalyticsService analyticsService,
                          TokenBucketRateLimiter rateLimiter) {
        this.urlShortenerService = urlShortenerService;
        this.analyticsService = analyticsService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest httpRequest) {

        // Rate limiting keyed by client IP — see TokenBucketRateLimiter for
        // the brownfield rationale.
        String clientKey = httpRequest.getRemoteAddr();
        if (!rateLimiter.tryConsume(clientKey)) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded for client " + clientKey + ". Try again shortly.");
        }

        CreateUrlResponse response = urlShortenerService.createShortUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        ShortUrl shortUrl = urlShortenerService.resolve(code);
        analyticsService.recordClick(code, request.getHeader("Referer"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LOCATION, shortUrl.getOriginalUrl());
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/urls/{code}/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(@PathVariable String code) {
        return ResponseEntity.ok(analyticsService.getAnalytics(code));
    }
}
