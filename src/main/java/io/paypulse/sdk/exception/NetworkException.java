package io.paypulse.sdk.exception;

/**
 * Thrown when an underlying I/O, DNS, or network timeout error occurs while connecting to the API.
 */
public class NetworkException extends PayPulseException {
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
