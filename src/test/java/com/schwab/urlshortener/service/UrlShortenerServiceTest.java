package com.schwab.urlshortener.service;

import com.schwab.urlshortener.dto.CreateUrlRequest;
import com.schwab.urlshortener.dto.CreateUrlResponse;
import com.schwab.urlshortener.exception.ShortUrlExpiredException;
import com.schwab.urlshortener.exception.ShortUrlNotFoundException;
import com.schwab.urlshortener.model.ShortUrl;
import com.schwab.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepository repository;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(repository, "http://localhost:8080");
    }

    @Test
    void createShortUrl_persistsAndAssignsCodeFromGeneratedId() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com/some/long/path");

        when(repository.findByOriginalUrl(request.getOriginalUrl())).thenReturn(Optional.empty());
        // First save: simulate the DB assigning id=123 via reflection-free approach —
        // we build a real entity and use a spy-like save that sets id via a second save call.
        ShortUrl savedWithId = new ShortUrl("PENDING", request.getOriginalUrl(), Instant.now(), null);
        setId(savedWithId, 123L);

        when(repository.save(any(ShortUrl.class)))
                .thenReturn(savedWithId)
                .thenAnswer(inv -> inv.getArgument(0));

        CreateUrlResponse response = service.createShortUrl(request);

        assertThat(response.getShortCode()).isEqualTo(Base62Encoder.encode(123L));
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/" + response.getShortCode());
        assertThat(response.getOriginalUrl()).isEqualTo(request.getOriginalUrl());
        verify(repository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    void createShortUrl_returnsExistingMappingForDuplicateUrl() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com/dup");

        ShortUrl existing = new ShortUrl("abc123", request.getOriginalUrl(), Instant.now(), null);
        when(repository.findByOriginalUrl(request.getOriginalUrl())).thenReturn(Optional.of(existing));

        CreateUrlResponse response = service.createShortUrl(request);

        assertThat(response.getShortCode()).isEqualTo("abc123");
        verify(repository, never()).save(any());
    }

    @Test
    void resolve_throwsNotFoundForUnknownCode() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("missing"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void resolve_throwsExpiredForPastTtl() {
        ShortUrl expired = new ShortUrl("exp123", "https://example.com",
                Instant.now().minus(2, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS));
        when(repository.findByShortCode("exp123")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolve("exp123"))
                .isInstanceOf(ShortUrlExpiredException.class);
    }

    @Test
    void resolve_returnsEntityForValidCode() {
        ShortUrl valid = new ShortUrl("ok123", "https://example.com", Instant.now(), null);
        when(repository.findByShortCode("ok123")).thenReturn(Optional.of(valid));

        ShortUrl result = service.resolve("ok123");

        assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
    }

    private void setId(ShortUrl entity, Long id) {
        try {
            var field = ShortUrl.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
