package io.paypulse.sdk;

import io.paypulse.sdk.config.PayPulseConfig;

import java.time.Duration;
import java.util.Objects;

/**
 * Main entry point for interacting with the PayPulse API.
 * <p>
 * Instantiate this client using the builder:
 * <pre>{@code
 * PayPulseClient client = PayPulseClient.builder()
 *     .apiKey("sk_test_12345")
 *     .timeout(Duration.ofSeconds(10))
 *     .build();
 * }</pre>
 */
public class PayPulseClient implements AutoCloseable {

    private final PayPulseConfig config;

    protected PayPulseClient(PayPulseConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public PayPulseConfig getConfig() {
        return config;
    }

    @Override
    public void close() {
        // Hook for resource cleanup when transport is attached
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl = PayPulseConfig.DEFAULT_BASE_URL;
        private Duration timeout = PayPulseConfig.DEFAULT_TIMEOUT;
        private int maxRetries = PayPulseConfig.DEFAULT_MAX_RETRIES;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public PayPulseClient build() {
            PayPulseConfig config = new PayPulseConfig(apiKey, baseUrl, timeout, maxRetries);
            return new PayPulseClient(config);
        }
    }
}
