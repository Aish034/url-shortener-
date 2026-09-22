package com.schwab.urlshortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public TokenBucketRateLimiter tokenBucketRateLimiter(
            @Value("${app.rate-limit.capacity}") long capacity,
            @Value("${app.rate-limit.refill-per-minute}") long refillPerMinute) {
        return new TokenBucketRateLimiter(capacity, refillPerMinute);
    }
}
