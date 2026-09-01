package io.paypulse.sdk.model.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Request payload for creating a refund on an existing Payment.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RefundRequest(
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("reason") String reason,
        @JsonIgnore String idempotencyKey
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BigDecimal amount;
        private String reason;
        private String idempotencyKey;

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder amount(String amountStr) {
            this.amount = new BigDecimal(amountStr);
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public RefundRequest build() {
            return new RefundRequest(amount, reason, idempotencyKey);
        }
    }
}
