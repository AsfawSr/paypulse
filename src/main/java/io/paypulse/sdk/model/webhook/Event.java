package io.paypulse.sdk.model.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Root domain entity representing an asynchronous webhook event sent by PayPulse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Event(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("data") EventData data,
        @JsonProperty("createdAt") Instant createdAt
) {
}
