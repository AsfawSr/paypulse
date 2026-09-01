package io.paypulse.sdk.model.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Status enumeration for PayPulse transactions.
 */
public enum PaymentStatus {
    @JsonProperty("pending")
    PENDING,

    @JsonProperty("succeeded")
    SUCCEEDED,

    @JsonProperty("failed")
    FAILED,

    @JsonProperty("refunded")
    REFUNDED
}
