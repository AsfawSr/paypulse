package io.paypulse.sdk;

import io.paypulse.sdk.config.PayPulseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayPulseClientTest {

    @Test
    @DisplayName("Should build client with valid API key and default settings")
    void shouldBuildClientWithDefaults() {
        PayPulseClient client = PayPulseClient.builder()
                .apiKey("sk_test_12345")
                .build();

        assertThat(client).isNotNull();
        assertThat(client.getConfig().apiKey()).isEqualTo("sk_test_12345");
        assertThat(client.getConfig().baseUrl()).isEqualTo(PayPulseConfig.DEFAULT_BASE_URL);
        assertThat(client.getConfig().timeout()).isEqualTo(PayPulseConfig.DEFAULT_TIMEOUT);
        assertThat(client.getConfig().maxRetries()).isEqualTo(PayPulseConfig.DEFAULT_MAX_RETRIES);
    }

    @Test
    @DisplayName("Should build client with custom configuration overrides")
    void shouldBuildClientWithCustomOverrides() {
        PayPulseClient client = PayPulseClient.builder()
                .apiKey("sk_live_secret")
                .baseUrl("https://sandbox.paypulse.io/v1")
                .timeout(Duration.ofSeconds(15))
                .maxRetries(5)
                .build();

        assertThat(client.getConfig().apiKey()).isEqualTo("sk_live_secret");
        assertThat(client.getConfig().baseUrl()).isEqualTo("https://sandbox.paypulse.io/v1");
        assertThat(client.getConfig().timeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(client.getConfig().maxRetries()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should fail when API key is missing or blank")
    void shouldFailWhenApiKeyIsMissingOrBlank() {
        assertThatThrownBy(() -> PayPulseClient.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("apiKey must not be null");

        assertThatThrownBy(() -> PayPulseClient.builder().apiKey("  ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apiKey must not be blank");
    }

    @Test
    @DisplayName("Should fail when maxRetries is negative")
    void shouldFailWhenMaxRetriesIsNegative() {
        assertThatThrownBy(() -> PayPulseClient.builder()
                .apiKey("sk_test_123")
                .maxRetries(-1)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRetries cannot be negative");
    }
}
