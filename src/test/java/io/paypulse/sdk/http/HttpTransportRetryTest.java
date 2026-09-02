package io.paypulse.sdk.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.paypulse.sdk.config.PayPulseConfig;
import io.paypulse.sdk.exception.AuthenticationException;
import io.paypulse.sdk.exception.RateLimitException;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpTransportRetryTest {

    private static WireMockServer wireMockServer;
    private HttpTransport transport;

    public record TestResult(String status) {}

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        PayPulseConfig config = new PayPulseConfig(
                "sk_test_retry_key",
                wireMockServer.baseUrl(),
                Duration.ofSeconds(5),
                2 // 2 retries allowed
        );
        // Using very short backoffs for fast test execution
        RetryPolicy fastRetryPolicy = new RetryPolicy(2, Duration.ofMillis(10), Duration.ofMillis(50), 2.0);
        transport = new HttpTransport(config, java.net.http.HttpClient.newHttpClient(), fastRetryPolicy);
    }

    @Test
    @DisplayName("Should successfully retry on 503 Service Unavailable and recover")
    void shouldRetryOn503AndRecover() {
        // WireMock Scenario: Attempt 1 returns 503, Attempt 2 returns 200 OK
        wireMockServer.stubFor(get(urlEqualTo("/transient"))
                .inScenario("Retry503")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503).withBody("{\"error\":\"Service Unavailable\"}"))
                .willSetStateTo("RECOVERED"));

        wireMockServer.stubFor(get(urlEqualTo("/transient"))
                .inScenario("Retry503")
                .whenScenarioStateIs("RECOVERED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\"}")));

        TestResult result = transport.get("/transient", TestResult.class);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("success");
        wireMockServer.verify(2, getRequestedFor(urlEqualTo("/transient")));
    }

    @Test
    @DisplayName("Should successfully retry asynchronously on 429 Too Many Requests and recover")
    void shouldRetryAsyncOn429AndRecover() {
        wireMockServer.stubFor(get(urlEqualTo("/async-rate-limit"))
                .inScenario("Async429")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "0")
                        .withBody("{\"error\":\"Rate limit exceeded\"}"))
                .willSetStateTo("RECOVERED"));

        wireMockServer.stubFor(get(urlEqualTo("/async-rate-limit"))
                .inScenario("Async429")
                .whenScenarioStateIs("RECOVERED")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"async_success\"}")));

        CompletableFuture<TestResult> future = transport.getAsync("/async-rate-limit", TestResult.class);
        TestResult result = future.join();

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("async_success");
        wireMockServer.verify(2, getRequestedFor(urlEqualTo("/async-rate-limit")));
    }

    @Test
    @DisplayName("Should NOT retry on non-transient 401 Unauthorized client errors")
    void shouldNotRetryOn401() {
        wireMockServer.stubFor(get(urlEqualTo("/auth-fail"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Unauthorized\",\"code\":\"invalid_key\"}")));

        assertThatThrownBy(() -> transport.get("/auth-fail", TestResult.class))
                .isInstanceOf(AuthenticationException.class);

        // Verify only 1 request was sent (no retries)
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/auth-fail")));
    }

    @Test
    @DisplayName("Should exhaust retries and throw exception when server persistently fails")
    void shouldExhaustRetriesAndThrowException() {
        wireMockServer.stubFor(get(urlEqualTo("/persistent-error"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "0")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Rate limit exceeded\",\"code\":\"rate_limit_exceeded\"}")));

        assertThatThrownBy(() -> transport.get("/persistent-error", TestResult.class))
                .isInstanceOf(RateLimitException.class);

        // Initial attempt + 2 retries = 3 total requests
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/persistent-error")));
    }
}
