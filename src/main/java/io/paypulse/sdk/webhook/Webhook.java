package io.paypulse.sdk.webhook;

import io.paypulse.sdk.exception.SignatureVerificationException;
import io.paypulse.sdk.http.HttpTransport;
import io.paypulse.sdk.model.webhook.Event;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility for cryptographically verifying and constructing PayPulse Webhook Events.
 */
public final class Webhook {

    public static final Duration DEFAULT_TOLERANCE = Duration.ofMinutes(5);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private Webhook() {
        // Utility class
    }

    /**
     * Verifies the cryptographic HMAC-SHA256 signature and parses the webhook JSON payload into an Event.
     *
     * @param payload the raw, unmodified JSON payload received in the HTTP request body
     * @param signatureHeader the value of the 'PayPulse-Signature' HTTP header
     * @param secret the webhook signing secret (e.g. whsec_...)
     * @return the verified and parsed Event
     * @throws SignatureVerificationException if the signature is invalid or timestamp has expired
     */
    public static Event constructEvent(String payload, String signatureHeader, String secret) {
        return constructEvent(payload, signatureHeader, secret, DEFAULT_TOLERANCE);
    }

    /**
     * Verifies the cryptographic signature with a custom timestamp tolerance.
     *
     * @param payload the raw request body
     * @param signatureHeader the 'PayPulse-Signature' header
     * @param secret the webhook secret
     * @param tolerance maximum allowed difference between event timestamp and current time (null to disable)
     * @return the verified Event
     * @throws SignatureVerificationException if verification fails
     */
    public static Event constructEvent(String payload, String signatureHeader, String secret, Duration tolerance) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(signatureHeader, "signatureHeader must not be null");
        Objects.requireNonNull(secret, "secret must not be null");

        if (payload.isBlank()) {
            throw new SignatureVerificationException("Webhook payload must not be blank", signatureHeader);
        }
        if (signatureHeader.isBlank()) {
            throw new SignatureVerificationException("Signature header must not be blank", signatureHeader);
        }
        if (secret.isBlank()) {
            throw new SignatureVerificationException("Webhook secret must not be blank", signatureHeader);
        }

        HeaderValues headerValues = parseSignatureHeader(signatureHeader);

        // Replay attack prevention: verify timestamp freshness
        if (tolerance != null) {
            long now = Instant.now().getEpochSecond();
            long diff = Math.abs(now - headerValues.timestamp);
            if (diff > tolerance.toSeconds()) {
                throw new SignatureVerificationException(
                        "Timestamp outside tolerance window (diff=" + diff + "s, max=" + tolerance.toSeconds() + "s)",
                        signatureHeader
                );
            }
        }

        // Compute expected HMAC-SHA256 signature
        String expectedSignature = computeHmacSha256(headerValues.timestamp + "." + payload, secret);

        // Perform constant-time comparison against all v1 signatures in header (supports secret rotation)
        boolean signatureValid = false;
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);

        for (String actualSignature : headerValues.signatures) {
            byte[] actualBytes = actualSignature.getBytes(StandardCharsets.UTF_8);
            if (MessageDigest.isEqual(expectedBytes, actualBytes)) {
                signatureValid = true;
                break;
            }
        }

        if (!signatureValid) {
            throw new SignatureVerificationException("No matching signature found in header", signatureHeader);
        }

        try {
            return HttpTransport.createDefaultObjectMapper().readValue(payload, Event.class);
        } catch (Exception e) {
            throw new SignatureVerificationException("Failed to parse webhook JSON payload: " + e.getMessage(), signatureHeader);
        }
    }

    /**
     * Helper to compute an HMAC-SHA256 signature string for a payload.
     */
    public static String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    /**
     * Generates a valid 'PayPulse-Signature' header value (useful for testing and simulators).
     */
    public static String generateSignatureHeader(String payload, String secret, long timestampSeconds) {
        String signature = computeHmacSha256(timestampSeconds + "." + payload, secret);
        return "t=" + timestampSeconds + ",v1=" + signature;
    }

    private static HeaderValues parseSignatureHeader(String header) {
        Long timestamp = null;
        List<String> signatures = new ArrayList<>();

        String[] pairs = header.split(",");
        for (String pair : pairs) {
            String[] parts = pair.trim().split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = parts[0].trim();
            String value = parts[1].trim();

            if ("t".equalsIgnoreCase(key)) {
                try {
                    timestamp = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    throw new SignatureVerificationException("Invalid timestamp in signature header: " + value, header);
                }
            } else if ("v1".equalsIgnoreCase(key)) {
                signatures.add(value);
            }
        }

        if (timestamp == null) {
            throw new SignatureVerificationException("Missing timestamp 't' in signature header", header);
        }
        if (signatures.isEmpty()) {
            throw new SignatureVerificationException("Missing signature 'v1' in signature header", header);
        }

        return new HeaderValues(timestamp, signatures);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private record HeaderValues(long timestamp, List<String> signatures) {}
}
