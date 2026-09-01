package io.paypulse.sdk.model.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Request payload for creating a new Customer.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateCustomerRequest(
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("metadata") Map<String, String> metadata
) {
    public CreateCustomerRequest {
        Objects.requireNonNull(name, "Customer name is required");
        Objects.requireNonNull(email, "Customer email is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        if (email.isBlank()) {
            throw new IllegalArgumentException("Customer email must not be blank");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String email;
        private Map<String, String> metadata;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder addMetadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public CreateCustomerRequest build() {
            return new CreateCustomerRequest(name, email, metadata);
        }
    }
}
