package io.paypulse.sdk;

import io.paypulse.sdk.config.PayPulseConfig;
import io.paypulse.sdk.http.HttpTransport;
import io.paypulse.sdk.resource.CustomersResource;
import io.paypulse.sdk.resource.PaymentsResource;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

/**
 * Main entry point for interacting with the PayPulse Payment and Customer API.
 * <p>
 * Example usage:
 * <pre>{@code
 * PayPulseClient client = PayPulseClient.builder()
 *     .apiKey("sk_test_12345")
 *     .timeout(Duration.ofSeconds(15))
 *     .build();
 *
 * Customer customer = client.customers().getById("cus_123");
 * }</pre>
 */
public class PayPulseClient implements AutoCloseable {

    private final PayPulseConfig config;
    private final HttpTransport transport;
    private final CustomersResource customers;
    private final PaymentsResource payments;

    protected PayPulseClient(PayPulseConfig config, HttpTransport transport) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        this.customers = new CustomersResource(this.transport);
        this.payments = new PaymentsResource(this.transport);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Access operations on the Customers API.
     */
    public CustomersResource customers() {
        return customers;
    }

    /**
     * Access operations on the Payments API.
     */
    public PaymentsResource payments() {
        return payments;
    }

    /**
     * Returns the active configuration for this client.
     */
    public PayPulseConfig getConfig() {
        return config;
    }

    /**
     * Returns the underlying HTTP transport engine.
     */
    public HttpTransport getTransport() {
        return transport;
    }

    @Override
    public void close() {
        // Reserved for shutting down any background connection pools if necessary
    }

    public static class Builder {
        private String apiKey;
        private String baseUrl = PayPulseConfig.DEFAULT_BASE_URL;
        private Duration timeout = PayPulseConfig.DEFAULT_TIMEOUT;
        private int maxRetries = PayPulseConfig.DEFAULT_MAX_RETRIES;
        private HttpClient customHttpClient;
        private io.paypulse.sdk.http.RetryPolicy customRetryPolicy;

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

        public Builder httpClient(HttpClient httpClient) {
            this.customHttpClient = httpClient;
            return this;
        }

        public Builder retryPolicy(io.paypulse.sdk.http.RetryPolicy retryPolicy) {
            this.customRetryPolicy = retryPolicy;
            return this;
        }

        public PayPulseClient build() {
            PayPulseConfig config = new PayPulseConfig(apiKey, baseUrl, timeout, maxRetries);
            HttpClient client = (customHttpClient != null)
                    ? customHttpClient
                    : HttpClient.newBuilder().connectTimeout(config.timeout()).build();
            io.paypulse.sdk.http.RetryPolicy policy = (customRetryPolicy != null)
                    ? customRetryPolicy
                    : new io.paypulse.sdk.http.RetryPolicy(config.maxRetries());

            HttpTransport transport = new HttpTransport(config, client, policy);
            return new PayPulseClient(config, transport);
        }
    }
}
