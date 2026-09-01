package io.paypulse.sdk.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.paypulse.sdk.http.HttpTransport;
import io.paypulse.sdk.model.common.Page;
import io.paypulse.sdk.model.customer.CreateCustomerRequest;
import io.paypulse.sdk.model.customer.Customer;
import io.paypulse.sdk.model.payment.ChargeRequest;
import io.paypulse.sdk.model.payment.Payment;
import io.paypulse.sdk.model.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelSerializationTest {

    private final ObjectMapper mapper = HttpTransport.createDefaultObjectMapper();

    @Test
    @DisplayName("Should serialize and deserialize Customer model properly")
    void shouldSerializeAndDeserializeCustomer() throws Exception {
        String json = """
                {
                    "id": "cus_12345",
                    "name": "Sarah Connor",
                    "email": "sarah@resistance.org",
                    "metadata": {"plan": "premium"},
                    "createdAt": "2026-08-31T12:00:00Z"
                }
                """;

        Customer customer = mapper.readValue(json, Customer.class);

        assertThat(customer.id()).isEqualTo("cus_12345");
        assertThat(customer.name()).isEqualTo("Sarah Connor");
        assertThat(customer.email()).isEqualTo("sarah@resistance.org");
        assertThat(customer.metadata()).containsEntry("plan", "premium");
        assertThat(customer.createdAt()).isEqualTo(Instant.parse("2026-08-31T12:00:00Z"));
    }

    @Test
    @DisplayName("Should serialize ChargeRequest to valid JSON")
    void shouldSerializeChargeRequest() throws Exception {
        ChargeRequest request = ChargeRequest.builder()
                .customerId("cus_12345")
                .amount(new BigDecimal("99.95"))
                .currency("usd")
                .description("Cloud Subscription")
                .idempotencyKey("idemp_111")
                .build();

        String json = mapper.writeValueAsString(request);

        assertThat(json).contains("\"customerId\":\"cus_12345\"");
        assertThat(json).contains("\"amount\":99.95");
        assertThat(json).contains("\"currency\":\"USD\"");
        assertThat(json).contains("\"description\":\"Cloud Subscription\"");
        assertThat(json).doesNotContain("idempotencyKey"); // idempotencyKey is in HTTP header, not body
    }

    @Test
    @DisplayName("Should reject invalid ChargeRequest with negative amount")
    void shouldRejectInvalidChargeRequest() {
        assertThatThrownBy(() -> ChargeRequest.builder()
                .customerId("cus_12345")
                .amount(new BigDecimal("-10.00"))
                .currency("USD")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be greater than zero");
    }

    @Test
    @DisplayName("Should deserialize paginated Page of Payments")
    void shouldDeserializePaginatedPayments() throws Exception {
        String json = """
                {
                    "data": [
                        {
                            "id": "pay_001",
                            "customerId": "cus_123",
                            "amount": 25.50,
                            "currency": "USD",
                            "status": "succeeded",
                            "createdAt": "2026-08-31T10:00:00Z"
                        }
                    ],
                    "hasMore": false,
                    "totalCount": 1
                }
                """;

        Page<?> page = mapper.readValue(json, Page.class);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.data()).hasSize(1);
    }
}
