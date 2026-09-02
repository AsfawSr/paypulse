package io.paypulse.sdk.model.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.paypulse.sdk.exception.PayPulseException;
import io.paypulse.sdk.http.HttpTransport;

/**
 * Encapsulates the payload data of a Webhook Event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventData(
        @JsonProperty("object") JsonNode object
) {
    /**
     * Deserializes the underlying event object to the requested domain class (e.g. Payment.class, Customer.class).
     *
     * @param targetClass the class to deserialize the payload into
     * @param <T> the type of the domain model
     * @return the deserialized domain entity
     */
    public <T> T getObject(Class<T> targetClass) {
        if (object == null) {
            return null;
        }
        try {
            return HttpTransport.createDefaultObjectMapper().treeToValue(object, targetClass);
        } catch (Exception e) {
            throw new PayPulseException("Failed to deserialize webhook event object to " + targetClass.getSimpleName(), e);
        }
    }
}
