package io.paypulse.sdk.exception;

/**
 * Thrown when an API request fails due to invalid, missing, or unauthorized API credentials (HTTP 401 / 403).
 */
public class AuthenticationException extends PayPulseException {
    public AuthenticationException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }
}
