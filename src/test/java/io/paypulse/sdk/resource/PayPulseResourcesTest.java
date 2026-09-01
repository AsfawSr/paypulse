package io.paypulse.sdk.resource;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.paypulse.sdk.PayPulseClient;
import io.paypulse.sdk.model.common.Page;
import io.paypulse.sdk.model.customer.CreateCustomerRequest;
import io.paypulse.sdk.model.customer.Customer;
import io.paypulse.sdk.model.customer.UpdateCustomerRequest;
import io.paypulse.sdk.model.payment.ChargeRequest;
import io.paypulse.sdk.model.payment.Payment;
import io.paypulse.sdk.model.payment.PaymentStatus;
import io.paypulse.sdk.model.payment.RefundRequest;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class PayPulseResourcesTest {

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
    @DisplayName("Should create customer through client.customers().create()")
    void shouldCreateCustomer() {
        wireMockServer.stubFor(post(urlEqualTo("/customers"))
                .withHeader("Authorization", equalTo("Bearer sk_test_mock_key"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "cus_999",
                                    "name": "Jane Doe",
                                    "email": "jane@example.com",
                                    "createdAt": "2026-08-31T12:00:00Z"
                                }
                                """)));

        Customer customer = client.customers().create(
                CreateCustomerRequest.builder()
                        .name("Jane Doe")
                        .email("jane@example.com")
                        .build()
        );

        assertThat(customer).isNotNull();
        assertThat(customer.id()).isEqualTo("cus_999");
        assertThat(customer.name()).isEqualTo("Jane Doe");
        assertThat(customer.email()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Should retrieve and update customer")
    void shouldRetrieveAndUpdateCustomer() {
        wireMockServer.stubFor(get(urlEqualTo("/customers/cus_999"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "cus_999",
                                    "name": "Jane Doe",
                                    "email": "jane@example.com"
                                }
                                """)));

        wireMockServer.stubFor(put(urlEqualTo("/customers/cus_999"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "cus_999",
                                    "name": "Jane Smith",
                                    "email": "jane.smith@example.com"
                                }
                                """)));

        Customer fetched = client.customers().getById("cus_999");
        assertThat(fetched.name()).isEqualTo("Jane Doe");

        Customer updated = client.customers().update("cus_999",
                UpdateCustomerRequest.builder()
                        .name("Jane Smith")
                        .email("jane.smith@example.com")
                        .build()
        );
        assertThat(updated.name()).isEqualTo("Jane Smith");
        assertThat(updated.email()).isEqualTo("jane.smith@example.com");
    }

    @Test
    @DisplayName("Should create charge and issue refund through client.payments()")
    void shouldChargeAndRefundPayment() {
        wireMockServer.stubFor(post(urlEqualTo("/payments"))
                .withHeader("Idempotency-Key", equalTo("idemp-charge-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "pay_555",
                                    "customerId": "cus_999",
                                    "amount": 49.99,
                                    "currency": "USD",
                                    "status": "succeeded",
                                    "description": "Pro Tier Plan",
                                    "createdAt": "2026-08-31T12:00:00Z"
                                }
                                """)));

        wireMockServer.stubFor(post(urlEqualTo("/payments/pay_555/refund"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "pay_555",
                                    "customerId": "cus_999",
                                    "amount": 49.99,
                                    "currency": "USD",
                                    "status": "refunded",
                                    "refundedAmount": 49.99,
                                    "createdAt": "2026-08-31T12:00:00Z"
                                }
                                """)));

        Payment payment = client.payments().charge(
                ChargeRequest.builder()
                        .customerId("cus_999")
                        .amount(new BigDecimal("49.99"))
                        .currency("USD")
                        .description("Pro Tier Plan")
                        .idempotencyKey("idemp-charge-123")
                        .build()
        );

        assertThat(payment).isNotNull();
        assertThat(payment.id()).isEqualTo("pay_555");
        assertThat(payment.status()).isEqualTo(PaymentStatus.SUCCEEDED);

        Payment refunded = client.payments().refund("pay_555",
                RefundRequest.builder()
                        .reason("Customer request")
                        .build()
        );

        assertThat(refunded.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.refundedAmount()).isEqualTo(new BigDecimal("49.99"));
    }

    @Test
    @DisplayName("Should list paginated payments with filtering")
    void shouldListPayments() {
        wireMockServer.stubFor(get(urlEqualTo("/payments?limit=10&customer_id=cus_999"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "data": [
                                        {
                                            "id": "pay_555",
                                            "customerId": "cus_999",
                                            "amount": 49.99,
                                            "currency": "USD",
                                            "status": "succeeded"
                                        }
                                    ],
                                    "hasMore": false,
                                    "totalCount": 1
                                }
                                """)));

        Page<Payment> page = client.payments().list(10, "cus_999", null);
        assertThat(page.data()).hasSize(1);
        assertThat(page.data().getFirst().id()).isEqualTo("pay_555");
    }
}
