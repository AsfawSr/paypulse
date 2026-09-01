package io.paypulse.sdk.exception;

/**
 * Base unchecked exception for all PayPulse SDK errors.
 */
public class PayPulseException extends RuntimeException {
    private final Integer statusCode;
    private final String errorCode;

    public PayPulseException(String message) {
        super(message);
        this.statusCode = null;
        this.errorCode = null;
    }

    public PayPulseException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.errorCode = null;
    }

    public PayPulseException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
