package io.paypulse.sdk.http;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Defines retry strategies, backoff calculations, and transient error detection for the SDK.
 */
public class RetryPolicy {

    public static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(500);
    public static final Duration DEFAULT_MAX_BACKOFF = Duration.ofSeconds(10);
    public static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(
            408, // Request Timeout
            429, // Too Many Requests (Rate Limit)
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504  // Gateway Timeout
    );

    private final int maxRetries;
    private final Duration initialBackoff;
    private final Duration maxBackoff;
    private final double backoffMultiplier;

    public RetryPolicy(int maxRetries) {
        this(maxRetries, DEFAULT_INITIAL_BACKOFF, DEFAULT_MAX_BACKOFF, DEFAULT_BACKOFF_MULTIPLIER);
    }

    public RetryPolicy(int maxRetries, Duration initialBackoff, Duration maxBackoff, double backoffMultiplier) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries cannot be negative");
        }
        this.maxRetries = maxRetries;
        this.initialBackoff = Objects.requireNonNullElse(initialBackoff, DEFAULT_INITIAL_BACKOFF);
        this.maxBackoff = Objects.requireNonNullElse(maxBackoff, DEFAULT_MAX_BACKOFF);
        this.backoffMultiplier = backoffMultiplier > 1.0 ? backoffMultiplier : DEFAULT_BACKOFF_MULTIPLIER;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Determines whether an HTTP response status code indicates a retryable condition.
     */
    public boolean isRetryableStatus(int statusCode) {
        return RETRYABLE_STATUS_CODES.contains(statusCode);
    }

    /**
     * Determines whether a caught exception represents a retryable I/O or network error.
     */
    public boolean isRetryableException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        return throwable instanceof IOException;
    }

    /**
     * Calculates the backoff delay for a given retry attempt (0-indexed).
     * <p>
     * If the response provides a "Retry-After" header, that duration is prioritized.
     * Otherwise, exponential backoff with full jitter is applied.
     */
    public Duration computeDelay(int attempt, HttpResponse<?> response) {
        if (response != null) {
            var retryAfterHeader = response.headers().firstValue("Retry-After");
            if (retryAfterHeader.isPresent()) {
                try {
                    long seconds = Long.parseLong(retryAfterHeader.get().trim());
                    if (seconds >= 0) {
                        return Duration.ofSeconds(seconds);
                    }
                } catch (NumberFormatException ignored) {
                    // Not an integer header value; fall through to exponential calculation
                }
            }
        }

        // Exponential delay: initialBackoff * (multiplier ^ attempt)
        double expDelayMs = initialBackoff.toMillis() * Math.pow(backoffMultiplier, attempt);
        long cappedDelayMs = Math.min((long) expDelayMs, maxBackoff.toMillis());

        // Apply full jitter (random value between 0 and cappedDelayMs) to prevent thundering herds
        long jitteredDelayMs = ThreadLocalRandom.current().nextLong(cappedDelayMs / 2, cappedDelayMs + 1);

        return Duration.ofMillis(Math.max(1, jitteredDelayMs));
    }
}
