package io.paypulse.sdk.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.paypulse.sdk.config.PayPulseConfig;
import io.paypulse.sdk.exception.*;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpTransportTest {

    private static WireMockServer wireMockServer;
    private HttpTransport transport;

    public record DummyEntity(String id, String name) {}

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
                "sk_test_mock123",
                wireMockServer.baseUrl(),
                Duration.ofSeconds(5),
                0
        );
        transport = new HttpTransport(config);
    }

    @Test
    @DisplayName("Should execute GET request and parse JSON response correctly")
    void shouldExecuteGetRequest() {
        wireMockServer.stubFor(get(urlEqualTo("/dummy/123"))
                .withHeader("Authorization", equalTo("Bearer sk_test_mock123"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"123\",\"name\":\"Test Entity\"}")));

        DummyEntity result = transport.get("/dummy/123", DummyEntity.class);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("123");
        assertThat(result.name()).isEqualTo("Test Entity");
    }

    @Test
    @DisplayName("Should execute POST request with JSON body and Idempotency-Key")
    void shouldExecutePostWithIdempotencyKey() {
        wireMockServer.stubFor(post(urlEqualTo("/dummy"))
                .withHeader("Authorization", equalTo("Bearer sk_test_mock123"))
                .withHeader("Content-Type", containing("application/json"))
                .withHeader("Idempotency-Key", equalTo("idemp-key-789"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"new-1\",\"name\":\"Created Entity\"}")));

        DummyEntity requestBody = new DummyEntity(null, "Created Entity");
        DummyEntity result = transport.post("/dummy", requestBody, DummyEntity.class, "idemp-key-789");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo("new-1");
        assertThat(result.name()).isEqualTo("Created Entity");
    }

    @Test
    @DisplayName("Should throw AuthenticationException on HTTP 401")
    void shouldThrowAuthenticationExceptionOn401() {
        wireMockServer.stubFor(get(urlEqualTo("/dummy/secret"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Invalid API key provided\",\"code\":\"invalid_api_key\",\"status\":401}")));

        assertThatThrownBy(() -> transport.get("/dummy/secret", DummyEntity.class))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid API key provided")
                .satisfies(e -> {
                    AuthenticationException ae = (AuthenticationException) e;
                    assertThat(ae.getStatusCode()).isEqualTo(401);
                    assertThat(ae.getErrorCode()).isEqualTo("invalid_api_key");
                });
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException on HTTP 404")
    void shouldThrowResourceNotFoundExceptionOn404() {
        wireMockServer.stubFor(get(urlEqualTo("/dummy/missing"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Customer not found\",\"code\":\"customer_not_found\",\"status\":404}")));

        assertThatThrownBy(() -> transport.get("/dummy/missing", DummyEntity.class))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found");
    }

    @Test
    @DisplayName("Should throw RateLimitException with Retry-After header on HTTP 429")
    void shouldThrowRateLimitExceptionOn429() {
        wireMockServer.stubFor(get(urlEqualTo("/dummy/rate-limit"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "30")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Too many requests\",\"code\":\"rate_limit_exceeded\",\"status\":429}")));

        assertThatThrownBy(() -> transport.get("/dummy/rate-limit", DummyEntity.class))
                .isInstanceOf(RateLimitException.class)
                .hasMessage("Too many requests")
                .satisfies(e -> {
                    RateLimitException rle = (RateLimitException) e;
                    assertThat(rle.getRetryAfterSeconds()).isEqualTo(30);
                });
    }

    @Test
    @DisplayName("Should throw ApiServerException on HTTP 500")
    void shouldThrowApiServerExceptionOn500() {
        wireMockServer.stubFor(get(urlEqualTo("/dummy/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Internal server error occurred\",\"code\":\"server_error\",\"status\":500}")));

        assertThatThrownBy(() -> transport.get("/dummy/error", DummyEntity.class))
                .isInstanceOf(ApiServerException.class)
                .hasMessage("Internal server error occurred");
    }
}
