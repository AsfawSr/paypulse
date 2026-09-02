package io.paypulse.sdk.resource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.paypulse.sdk.PayPulseClient;
import io.paypulse.sdk.exception.AuthenticationException;
import io.paypulse.sdk.exception.ResourceNotFoundException;
import io.paypulse.sdk.model.customer.CreateCustomerRequest;
import io.paypulse.sdk.model.customer.Customer;
import io.paypulse.sdk.model.payment.ChargeRequest;
import io.paypulse.sdk.model.payment.Payment;
import io.paypulse.sdk.model.payment.PaymentStatus;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayPulseResourcesAsyncTest {

    private static WireMockServer wireMockServer;
    private PayPulseClient client;

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
        client = PayPulseClient.builder()
                .apiKey("sk_test_mock_key")
                .baseUrl(wireMockServer.baseUrl())
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @Test
    @DisplayName("Should create customer asynchronously using CompletableFuture")
    void shouldCreateCustomerAsync() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/customers"))
                .withHeader("Authorization", equalTo("Bearer sk_test_mock_key"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "cus_async_1",
                                    "name": "Async Alice",
                                    "email": "alice@example.com",
                                    "createdAt": "2026-08-31T12:00:00Z"
                                }
                                """)));

        CompletableFuture<Customer> future = client.customers().createAsync(
                CreateCustomerRequest.builder()
                        .name("Async Alice")
                        .email("alice@example.com")
                        .build()
        );

        Customer customer = future.get(); // block to verify async result

        assertThat(customer).isNotNull();
        assertThat(customer.id()).isEqualTo("cus_async_1");
        assertThat(customer.name()).isEqualTo("Async Alice");
    }

    @Test
    @DisplayName("Should charge payment asynchronously using CompletableFuture")
    void shouldChargePaymentAsync() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/payments"))
                .withHeader("Idempotency-Key", equalTo("idemp-async-999"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "pay_async_77",
                                    "customerId": "cus_async_1",
                                    "amount": 199.00,
                                    "currency": "USD",
                                    "status": "succeeded",
                                    "createdAt": "2026-08-31T12:00:00Z"
                                }
                                """)));

        CompletableFuture<Payment> future = client.payments().chargeAsync(
                ChargeRequest.builder()
                        .customerId("cus_async_1")
                        .amount(new BigDecimal("199.00"))
                        .currency("USD")
                        .idempotencyKey("idemp-async-999")
                        .build()
        );

        Payment payment = future.join();

        assertThat(payment).isNotNull();
        assertThat(payment.id()).isEqualTo("pay_async_77");
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("Should complete exceptionally with AuthenticationException on 401")
    void shouldCompleteExceptionallyOn401() {
        wireMockServer.stubFor(get(urlEqualTo("/customers/cus_secret"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Unauthorized\",\"code\":\"invalid_key\",\"status\":401}")));

        CompletableFuture<Customer> future = client.customers().getByIdAsync("cus_secret");

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    @DisplayName("Should complete exceptionally with ResourceNotFoundException on 404")
    void shouldCompleteExceptionallyOn404() {
        wireMockServer.stubFor(get(urlEqualTo("/customers/cus_missing"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Customer not found\",\"code\":\"not_found\",\"status\":404}")));

        CompletableFuture<Customer> future = client.customers().getByIdAsync("cus_missing");

        assertThatThrownBy(future::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }
}
