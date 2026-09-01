package io.paypulse.sdk.model.payment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Request payload for creating a new Payment charge.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChargeRequest(
        @JsonProperty("customerId") String customerId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("description") String description,
        @JsonIgnore String idempotencyKey
) {
    public ChargeRequest {
        Objects.requireNonNull(customerId, "customerId is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");

        if (customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code (e.g. USD, EUR)");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String customerId;
        private BigDecimal amount;
        private String currency = "USD";
        private String description;
        private String idempotencyKey;

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder amount(String amountStr) {
            this.amount = new BigDecimal(amountStr);
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency != null ? currency.toUpperCase() : null;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public ChargeRequest build() {
            return new ChargeRequest(customerId, amount, currency, description, idempotencyKey);
        }
    }
}
