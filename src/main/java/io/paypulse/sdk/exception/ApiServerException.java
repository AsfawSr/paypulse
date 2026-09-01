package io.paypulse.sdk.exception;

/**
 * Thrown when the PayPulse API server returns an internal error (HTTP 5xx).
 */
public class ApiServerException extends PayPulseException {
    public ApiServerException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }
}
