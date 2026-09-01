# PayPulse Java SDK

The official, type-safe Java SDK for integrating with the **PayPulse Payment & Customer Management API**.

Built for **Java 21+**, leveraging immutable `record`s, the native `HttpClient`, zero heavy transitive dependencies, and fluent builder patterns.

---

## 📦 Installation

### Maven
Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.paypulse</groupId>
    <artifactId>paypulse-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.paypulse:paypulse-java-sdk:1.0.0'
```

---

## ⚡ Quickstart

### 1. Initialize the Client

```java
import io.paypulse.sdk.PayPulseClient;
import java.time.Duration;

PayPulseClient client = PayPulseClient.builder()
    .apiKey("sk_test_51MzQ...yourApiKey")
    .timeout(Duration.ofSeconds(10))
    .maxRetries(3)
    .build();
```

---

### 2. Managing Customers

#### Create a Customer
```java
import io.paypulse.sdk.model.customer.CreateCustomerRequest;
import io.paypulse.sdk.model.customer.Customer;

Customer customer = client.customers().create(
    CreateCustomerRequest.builder()
        .name("Alex Johnson")
        .email("alex.johnson@example.com")
        .addMetadata("plan", "enterprise")
        .build()
);

System.out.println("Created Customer ID: " + customer.id());
```

#### Retrieve a Customer
```java
Customer customer = client.customers().getById("cus_12345");
```

#### Update a Customer
```java
import io.paypulse.sdk.model.customer.UpdateCustomerRequest;

Customer updated = client.customers().update("cus_12345",
    UpdateCustomerRequest.builder()
        .name("Alex M. Johnson")
        .build()
);
```

#### List Customers (Paginated)
```java
import io.paypulse.sdk.model.common.Page;

Page<Customer> page = client.customers().list(20, null);
for (Customer c : page.data()) {
    System.out.println(c.name() + " (" + c.email() + ")");
}
```

---

### 3. Processing Payments & Refunds

#### Charge a Customer
```java
import io.paypulse.sdk.model.payment.ChargeRequest;
import io.paypulse.sdk.model.payment.Payment;
import java.math.BigDecimal;

Payment payment = client.payments().charge(
    ChargeRequest.builder()
        .customerId(customer.id())
        .amount(new BigDecimal("49.99"))
        .currency("USD")
        .description("Monthly Cloud Subscription")
        .idempotencyKey("idemp_unique_key_123") // Safe retry deduplication
        .build()
);

System.out.println("Payment Status: " + payment.status()); // SUCCEEDED
```

#### Refund a Payment
```java
import io.paypulse.sdk.model.payment.RefundRequest;

Payment refunded = client.payments().refund(payment.id(),
    RefundRequest.builder()
        .amount(new BigDecimal("49.99"))
        .reason("Customer requested refund")
        .build()
);
```

---

## 🛡️ Error Handling

The SDK throws descriptive, typed unchecked exceptions mapped directly from API status codes:

```java
import io.paypulse.sdk.exception.*;

try {
    Payment payment = client.payments().charge(chargeRequest);
} catch (AuthenticationException e) {
    // HTTP 401 / 403: Invalid API key or permission error
    System.err.println("Auth failed: " + e.getMessage() + " (Code: " + e.getErrorCode() + ")");
} catch (ResourceNotFoundException e) {
    // HTTP 404: Customer or Payment ID was not found
    System.err.println("Not found: " + e.getMessage());
} catch (ValidationException e) {
    // HTTP 400 / 422: Invalid parameters or bad payload
    System.err.println("Validation error: " + e.getMessage());
} catch (RateLimitException e) {
    // HTTP 429: Rate limit exceeded
    System.err.println("Rate limited. Retry after " + e.getRetryAfterSeconds() + " seconds.");
} catch (ApiServerException e) {
    // HTTP 5xx: Server-side outage
    System.err.println("PayPulse service is currently unavailable.");
} catch (NetworkException e) {
    // Connectivity / timeout / DNS failure
    System.err.println("Connection failed: " + e.getMessage());
} catch (PayPulseException e) {
    // Catch-all base exception
    System.err.println("PayPulse error: " + e.getMessage());
}
```

---

## 🏗️ Architecture & SDK Design Highlights

```
src/main/java/io/paypulse/sdk/
├── config/
│   └── PayPulseConfig.java          # Immutable client configuration record
├── exception/                       # Custom typed exception hierarchy
│   ├── PayPulseException.java
│   ├── AuthenticationException.java
│   ├── ResourceNotFoundException.java
│   ├── ValidationException.java
│   ├── RateLimitException.java
│   ├── ApiServerException.java
│   └── NetworkException.java
├── http/
│   ├── HttpTransport.java           # Native Java 21 HttpClient & Jackson engine
│   └── ErrorResponse.java           # JSON error payload DTO
├── model/                           # Immutable Java 21 Records
│   ├── common/Page.java
│   ├── customer/
│   └── payment/
├── resource/                        # Focused endpoint services
│   ├── CustomersResource.java
│   └── PaymentsResource.java
├── example/
│   └── ExampleApp.java              # Runnable quickstart demo
└── PayPulseClient.java              # Main SDK entry point & Builder
```

---

## 🧪 Running Tests & Building

Run all automated unit and WireMock integration tests:
```bash
mvn clean test
```

Package the SDK JAR:
```bash
mvn clean package
```
