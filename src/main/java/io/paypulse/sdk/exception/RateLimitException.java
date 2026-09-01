package io.paypulse.sdk.exception;

/**
 * Thrown when API rate limits are exceeded (HTTP 429).
 */
public class RateLimitException extends PayPulseException {
    private final Integer retryAfterSeconds;

    public RateLimitException(String message, int statusCode, String errorCode, Integer retryAfterSeconds) {
        super(message, statusCode, errorCode);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
