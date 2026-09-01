package io.paypulse.sdk.exception;

/**
 * Thrown when request parameters or payload fail API validation (HTTP 400 / 422).
 */
public class ValidationException extends PayPulseException {
    public ValidationException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }
}
