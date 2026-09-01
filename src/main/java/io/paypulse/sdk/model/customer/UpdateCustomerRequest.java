package io.paypulse.sdk.model.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Request payload for updating an existing Customer.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateCustomerRequest(
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("metadata") Map<String, String> metadata
) {
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

        public UpdateCustomerRequest build() {
            return new UpdateCustomerRequest(name, email, metadata);
        }
    }
}
