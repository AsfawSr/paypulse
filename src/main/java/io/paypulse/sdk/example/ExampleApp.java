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

        // 1. Instantiate and configure the client
        PayPulseClient client = PayPulseClient.builder()
                .apiKey("sk_test_51MzQ...exampleKey")
                .timeout(Duration.ofSeconds(10))
                .maxRetries(3)
                .build();

        System.out.println("Client initialized with base URL: " + client.getConfig().baseUrl());
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

        System.out.println();
        System.out.println("SDK Execution complete.");
    }
}
