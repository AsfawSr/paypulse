package io.paypulse.sdk.webhook;

import io.paypulse.sdk.exception.SignatureVerificationException;
import io.paypulse.sdk.model.payment.Payment;
import io.paypulse.sdk.model.payment.PaymentStatus;
import io.paypulse.sdk.model.webhook.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookTest {

    private static final String SECRET = "whsec_test_secret_key_123456789";
    private static final String SAMPLE_PAYLOAD = """
            {
                "id": "evt_998877",
                "type": "payment.succeeded",
                "createdAt": "2026-08-31T12:00:00Z",
                "data": {
                    "object": {
                        "id": "pay_112233",
                        "customerId": "cus_555",
                        "amount": 99.99,
                        "currency": "USD",
                        "status": "succeeded",
                        "description": "Premium Plan"
                    }
                }
            }
            """;

    @Test
    @DisplayName("Should successfully verify and parse a valid signed webhook event")
    void shouldVerifyValidWebhook() {
        long now = Instant.now().getEpochSecond();
        String header = Webhook.generateSignatureHeader(SAMPLE_PAYLOAD, SECRET, now);

        Event event = Webhook.constructEvent(SAMPLE_PAYLOAD, header, SECRET);

        assertThat(event).isNotNull();
        assertThat(event.id()).isEqualTo("evt_998877");
        assertThat(event.type()).isEqualTo("payment.succeeded");

        // Verify deserializing nested event data object to typed Payment record
        Payment payment = event.data().getObject(Payment.class);
        assertThat(payment).isNotNull();
        assertThat(payment.id()).isEqualTo("pay_112233");
        assertThat(payment.amount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("Should reject webhook if payload was tampered with")
    void shouldRejectTamperedPayload() {
        long now = Instant.now().getEpochSecond();
        String header = Webhook.generateSignatureHeader(SAMPLE_PAYLOAD, SECRET, now);

        String tamperedPayload = SAMPLE_PAYLOAD.replace("99.99", "1.99");

        assertThatThrownBy(() -> Webhook.constructEvent(tamperedPayload, header, SECRET))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("No matching signature found in header");
    }

    @Test
    @DisplayName("Should reject webhook if signed with a different secret")
    void shouldRejectWrongSecret() {
        long now = Instant.now().getEpochSecond();
        String header = Webhook.generateSignatureHeader(SAMPLE_PAYLOAD, "whsec_wrong_secret", now);

        assertThatThrownBy(() -> Webhook.constructEvent(SAMPLE_PAYLOAD, header, SECRET))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("No matching signature found in header");
    }

    @Test
    @DisplayName("Should reject webhook if timestamp is older than tolerance window (replay attack protection)")
    void shouldRejectExpiredTimestamp() {
        long oldTimestamp = Instant.now().getEpochSecond() - 600; // 10 minutes ago (> 5 min tolerance)
        String header = Webhook.generateSignatureHeader(SAMPLE_PAYLOAD, SECRET, oldTimestamp);

        assertThatThrownBy(() -> Webhook.constructEvent(SAMPLE_PAYLOAD, header, SECRET, Duration.ofMinutes(5)))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Timestamp outside tolerance window");
    }

    @Test
    @DisplayName("Should verify webhook successfully when header contains multiple signatures for key rollover")
    void shouldVerifyWithMultipleSignatures() {
        long now = Instant.now().getEpochSecond();
        String validSignature = Webhook.computeHmacSha256(now + "." + SAMPLE_PAYLOAD, SECRET);
        String header = "t=" + now + ",v1=invalid_old_sig,v1=" + validSignature;

        Event event = Webhook.constructEvent(SAMPLE_PAYLOAD, header, SECRET);

        assertThat(event).isNotNull();
        assertThat(event.id()).isEqualTo("evt_998877");
    }

    @Test
    @DisplayName("Should reject malformed signature headers")
    void shouldRejectMalformedHeader() {
        assertThatThrownBy(() -> Webhook.constructEvent(SAMPLE_PAYLOAD, "invalid_header_format", SECRET))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Missing timestamp 't'");

        assertThatThrownBy(() -> Webhook.constructEvent(SAMPLE_PAYLOAD, "t=12345", SECRET))
                .isInstanceOf(SignatureVerificationException.class)
                .hasMessageContaining("Missing signature 'v1'");
    }
}
