package io.paypulse.sdk.model.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment domain entity returned by the PayPulse API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Payment(
        @JsonProperty("id") String id,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") PaymentStatus status,
        @JsonProperty("description") String description,
        @JsonProperty("refundedAmount") BigDecimal refundedAmount,
        @JsonProperty("createdAt") Instant createdAt
) {
}
