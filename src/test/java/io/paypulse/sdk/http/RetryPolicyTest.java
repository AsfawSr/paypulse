package io.paypulse.sdk.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryPolicyTest {

    @Test
    @DisplayName("Should correctly identify retryable and non-retryable HTTP status codes")
    void shouldIdentifyRetryableStatusCodes() {
        RetryPolicy policy = new RetryPolicy(3);

        // Retryable
        assertThat(policy.isRetryableStatus(429)).isTrue();
        assertThat(policy.isRetryableStatus(500)).isTrue();
        assertThat(policy.isRetryableStatus(502)).isTrue();
        assertThat(policy.isRetryableStatus(503)).isTrue();
        assertThat(policy.isRetryableStatus(504)).isTrue();

        // Non-retryable
        assertThat(policy.isRetryableStatus(200)).isFalse();
        assertThat(policy.isRetryableStatus(400)).isFalse();
        assertThat(policy.isRetryableStatus(401)).isFalse();
        assertThat(policy.isRetryableStatus(403)).isFalse();
        assertThat(policy.isRetryableStatus(404)).isFalse();
        assertThat(policy.isRetryableStatus(422)).isFalse();
    }

    @Test
    @DisplayName("Should identify IOException as retryable and NullPointerException as non-retryable")
    void shouldIdentifyRetryableExceptions() {
        RetryPolicy policy = new RetryPolicy(2);

        assertThat(policy.isRetryableException(new IOException("Connection reset"))).isTrue();
        assertThat(policy.isRetryableException(new NullPointerException())).isFalse();
        assertThat(policy.isRetryableException(null)).isFalse();
    }

    @Test
    @DisplayName("Should prioritize Retry-After header duration if present")
    void shouldPrioritizeRetryAfterHeader() {
        RetryPolicy policy = new RetryPolicy(3);

        HttpResponse<String> dummyResponse = new HttpResponse<>() {
            @Override public int statusCode() { return 429; }
            @Override public HttpRequest request() { return null; }
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
            @Override public HttpHeaders headers() {
                return HttpHeaders.of(Map.of("Retry-After", List.of("5")), (k, v) -> true);
            }
            @Override public String body() { return ""; }
            @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
            @Override public URI uri() { return null; }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2; }
        };

        Duration delay = policy.computeDelay(0, dummyResponse);

        assertThat(delay).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Should calculate exponential delay with jitter when Retry-After is absent")
    void shouldCalculateExponentialDelayWithJitter() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(100), Duration.ofSeconds(5), 2.0);

        Duration delay0 = policy.computeDelay(0, null);
        Duration delay1 = policy.computeDelay(1, null);

        assertThat(delay0.toMillis()).isGreaterThan(0).isLessThanOrEqualTo(100);
        assertThat(delay1.toMillis()).isGreaterThan(0).isLessThanOrEqualTo(200);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on negative maxRetries")
    void shouldRejectNegativeMaxRetries() {
        assertThatThrownBy(() -> new RetryPolicy(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRetries cannot be negative");
    }
}
