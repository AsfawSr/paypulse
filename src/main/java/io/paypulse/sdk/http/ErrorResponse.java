package io.paypulse.sdk.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard API error response payload returned by PayPulse servers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("code") String code,
        @JsonProperty("status") Integer status,
        @JsonProperty("message") String message
) {
    public String resolveMessage() {
        if (message != null && !message.isBlank()) {
            return message;
        }
        if (error != null && !error.isBlank()) {
            return error;
        }
        return "Unknown error occurred";
    }
}
