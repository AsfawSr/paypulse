package io.paypulse.sdk.model.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Customer domain entity returned by the PayPulse API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Customer(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("email") String email,
        @JsonProperty("metadata") Map<String, String> metadata,
        @JsonProperty("createdAt") Instant createdAt
) {
    public Customer {
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }
    }
}
