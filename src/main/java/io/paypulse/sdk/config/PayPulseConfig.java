package io.paypulse.sdk.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration settings for the PayPulse SDK client.
 */
public record PayPulseConfig(
        String apiKey,
        String baseUrl,
        Duration timeout,
        int maxRetries
) {
    public static final String DEFAULT_BASE_URL = "https://api.paypulse.io/v1";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    public static final int DEFAULT_MAX_RETRIES = 2;

    public PayPulseConfig {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative");
        }
    }
}
