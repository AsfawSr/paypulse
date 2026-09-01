package io.paypulse.sdk.exception;

/**
 * Thrown when a requested resource (e.g. customer ID, payment ID) does not exist (HTTP 404).
 */
public class ResourceNotFoundException extends PayPulseException {
    public ResourceNotFoundException(String message, int statusCode, String errorCode) {
        super(message, statusCode, errorCode);
    }
}
