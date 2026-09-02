package io.paypulse.sdk.exception;

/**
 * Thrown when a webhook payload fails cryptographic HMAC-SHA256 signature verification or timestamp replay validation.
 */
public class SignatureVerificationException extends PayPulseException {
    private final String signatureHeader;

    public SignatureVerificationException(String message) {
        super(message);
        this.signatureHeader = null;
    }

    public SignatureVerificationException(String message, String signatureHeader) {
        super(message);
        this.signatureHeader = signatureHeader;
    }

    public String getSignatureHeader() {
        return signatureHeader;
    }
}
