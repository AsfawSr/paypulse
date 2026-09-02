package io.paypulse.sdk.example;

import io.paypulse.sdk.PayPulseClient;
import io.paypulse.sdk.exception.AuthenticationException;
import io.paypulse.sdk.exception.PayPulseException;
import io.paypulse.sdk.exception.RateLimitException;
import io.paypulse.sdk.exception.ResourceNotFoundException;
import io.paypulse.sdk.model.customer.CreateCustomerRequest;
import io.paypulse.sdk.model.customer.Customer;
import io.paypulse.sdk.model.payment.ChargeRequest;
import io.paypulse.sdk.model.payment.Payment;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Example demonstrating how an external developer will use the PayPulse SDK in their application.
 */
public class ExampleApp {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     PayPulse Java SDK - Developer Quickstart     ");
        System.out.println("==================================================");

        // 1. Instantiate and configure the client with custom Retry Policy
        PayPulseClient client = PayPulseClient.builder()
                .apiKey("sk_test_51MzQ...exampleKey")
                .timeout(Duration.ofSeconds(10))
                .maxRetries(3)
                .retryPolicy(new io.paypulse.sdk.http.RetryPolicy(
                        3,
                        Duration.ofMillis(500),
                        Duration.ofSeconds(5),
                        2.0
                ))
                .build();

        System.out.println("Client initialized with base URL: " + client.getConfig().baseUrl());
        System.out.println("Automatic Retries Enabled: " + client.getTransport().getRetryPolicy().getMaxRetries() + " max attempts");
        System.out.println();

        // 2. Example: Building a Customer Request
        CreateCustomerRequest customerRequest = CreateCustomerRequest.builder()
                .name("Alex Johnson")
                .email("alex.johnson@example.com")
                .addMetadata("plan", "enterprise")
                .addMetadata("tier", "gold")
                .build();

        System.out.println("[1] Prepared Customer Creation Payload:");
        System.out.println("    Name:  " + customerRequest.name());
        System.out.println("    Email: " + customerRequest.email());
        System.out.println("    Meta:  " + customerRequest.metadata());
        System.out.println();

        // 3. Example: Building a Charge Request with Idempotency Key
        ChargeRequest chargeRequest = ChargeRequest.builder()
                .customerId("cus_demo123")
                .amount(new BigDecimal("149.99"))
                .currency("USD")
                .description("Annual SaaS Subscription")
                .idempotencyKey("idemp_unique_txn_987654")
                .build();

        System.out.println("[2] Prepared Payment Charge Payload:");
        System.out.println("    Customer ID: " + chargeRequest.customerId());
        System.out.println("    Amount:      $" + chargeRequest.amount() + " " + chargeRequest.currency());
        System.out.println("    Idempotency: " + chargeRequest.idempotencyKey());
        System.out.println();

        // 4. Example: Clean, Typed Exception Handling Pattern
        System.out.println("[3] Demonstrating Typed Exception Handling:");
        try {
            // Attempting to call the real remote API with a dummy key to demonstrate exception handling
            Customer customer = client.customers().getById("cus_non_existent");
            System.out.println("Customer found: " + customer.name());
        } catch (AuthenticationException e) {
            System.out.println("    [Auth Error]: Invalid API Key or Unauthorized (" + e.getStatusCode() + ") - " + e.getMessage());
        } catch (ResourceNotFoundException e) {
            System.out.println("    [Not Found Error]: Resource was not found (" + e.getStatusCode() + ") - " + e.getMessage());
        } catch (RateLimitException e) {
            System.out.println("    [Rate Limit Error]: Too many requests. Retry after: " + e.getRetryAfterSeconds() + " seconds");
        } catch (PayPulseException e) {
            System.out.println("    [PayPulse Error]: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("    [Unexpected Error]: " + e.getMessage());
        }

        // 5. Example: Asynchronous Non-Blocking Invocation (CompletableFuture)
        System.out.println("[4] Demonstrating Asynchronous Non-Blocking Invocation:");
        java.util.concurrent.CompletableFuture<Payment> asyncFuture = client.payments().chargeAsync(chargeRequest);

        asyncFuture.thenAccept(payment -> {
            System.out.println("    [Async Success]: Charged " + payment.id());
        }).exceptionally(ex -> {
            System.out.println("    [Async Expected Failure]: " + ex.getMessage());
            return null;
        });

        // 6. Example: Webhook Cryptographic Verification (HMAC-SHA256)
        System.out.println("[5] Demonstrating Webhook Signature Verification:");
        String sampleWebhookJson = """
                {
                    "id": "evt_live_12345",
                    "type": "payment.succeeded",
                    "createdAt": "2026-08-31T12:00:00Z",
                    "data": {
                        "object": {
                            "id": "pay_live_789",
                            "customerId": "cus_demo123",
                            "amount": 149.99,
                            "currency": "USD",
                            "status": "succeeded"
                        }
                    }
                }
                """;
        String webhookSecret = "whsec_test_secret_abc123";
        long now = java.time.Instant.now().getEpochSecond();
        String validSigHeader = io.paypulse.sdk.webhook.Webhook.generateSignatureHeader(sampleWebhookJson, webhookSecret, now);

        io.paypulse.sdk.model.webhook.Event event = io.paypulse.sdk.webhook.Webhook.constructEvent(
                sampleWebhookJson,
                validSigHeader,
                webhookSecret
        );

        System.out.println("    [Webhook Verified]: Event ID " + event.id() + " of type '" + event.type() + "'");
        Payment webhookPayment = event.data().getObject(Payment.class);
        System.out.println("    [Parsed Event Model]: Payment ID " + webhookPayment.id() + " -> Status: " + webhookPayment.status());

        System.out.println();
        System.out.println("SDK Execution complete.");
    }
}
