package com.schwab.urlshortener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class Base62EncoderTest {

    @Test
    void encodesZeroToFirstAlphabetChar() {
        assertThat(Base62Encoder.encode(0)).isEqualTo("0");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 61, 62, 100, 999999, Long.MAX_VALUE / 2})
    void encodeThenDecodeRoundTrips(long id) {
        String encoded = Base62Encoder.encode(id);
        long decoded = Base62Encoder.decode(encoded);
        assertThat(decoded).isEqualTo(id);
    }

    @Test
    void differentIdsProduceDifferentCodes() {
        assertThat(Base62Encoder.encode(1)).isNotEqualTo(Base62Encoder.encode(2));
    }
}
