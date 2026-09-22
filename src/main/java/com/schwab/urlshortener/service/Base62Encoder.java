package com.schwab.urlshortener.service;

/**
 * Base62 encoder for turning a DB-assigned numeric ID into a short,
 * URL-safe code.
 *
 * GREENFIELD SCENARIO — design decision (see SCENARIOS.md):
 * Two options were considered for short-code generation:
 *   1) Random string + collision check (retry on collision)
 *   2) Base62 encoding of the auto-increment primary key
 * AI first suggested option 1 (random 7-char string, check existsByShortCode,
 * retry up to N times). That was REJECTED as the primary strategy: under
 * concurrent writes it needs a retry loop and an extra DB round-trip per
 * collision, and collision probability grows with table size.
 * Option 2 was chosen instead: encoding the primary key is deterministic,
 * collision-free by construction, and O(1) — no retry loop needed. The
 * trade-off accepted: short codes are sequential/guessable in the sense that
 * one can enumerate them, which is fine for this assignment's scope (no
 * private/unlisted-link security requirement was stated) but would need
 * revisiting (e.g. adding a random salt or minimum-length padding permutation)
 * for a product with that requirement.
 */
public final class Base62Encoder {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    private Base62Encoder() {
    }

    public static String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long value = id;
        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String code) {
        long result = 0;
        for (char c : code.toCharArray()) {
            result = result * BASE + ALPHABET.indexOf(c);
        }
        return result;
    }
}
