package io.paypulse.sdk.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import io.paypulse.sdk.http.HttpTransport;
import io.paypulse.sdk.model.common.Page;
import io.paypulse.sdk.model.customer.CreateCustomerRequest;
import io.paypulse.sdk.model.customer.Customer;
import io.paypulse.sdk.model.customer.UpdateCustomerRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Resource service providing operations for managing PayPulse Customers.
 */
public class CustomersResource {

    private final HttpTransport transport;

    public CustomersResource(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
    }

    /**
     * Creates a new customer.
     *
     * @param request the customer creation payload
     * @return the newly created Customer
     */
    public Customer create(CreateCustomerRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return transport.post("/customers", request, Customer.class);
    }

    /**
     * Retrieves a customer by their unique ID.
     *
     * @param customerId the customer identifier
     * @return the Customer entity
     */
    public Customer getById(String customerId) {
        validateId(customerId, "customerId");
        return transport.get("/customers/" + encode(customerId), Customer.class);
    }

    /**
     * Updates an existing customer's information.
     *
     * @param customerId the customer identifier
     * @param request the update payload
     * @return the updated Customer entity
     */
    public Customer update(String customerId, UpdateCustomerRequest request) {
        validateId(customerId, "customerId");
        Objects.requireNonNull(request, "request must not be null");
        return transport.put("/customers/" + encode(customerId), request, Customer.class);
    }

    /**
     * Deletes a customer by their unique ID.
     *
     * @param customerId the customer identifier
     */
    public void delete(String customerId) {
        validateId(customerId, "customerId");
        transport.delete("/customers/" + encode(customerId));
    }

    /**
     * Lists customers with default pagination.
     *
     * @return a Page of Customer entities
     */
    public Page<Customer> list() {
        return list(20, null);
    }

    /**
     * Lists customers with pagination parameters.
     *
     * @param limit maximum number of items to return (1 - 100)
     * @param startingAfter cursor pointer for pagination
     * @return a Page of Customer entities
     */
    public Page<Customer> list(int limit, String startingAfter) {
        StringBuilder path = new StringBuilder("/customers?limit=").append(limit);
        if (startingAfter != null && !startingAfter.isBlank()) {
            path.append("&starting_after=").append(encode(startingAfter));
        }
        return transport.get(path.toString(), new TypeReference<Page<Customer>>() {});
    }

    // ==========================================
    // Asynchronous API Methods
    // ==========================================

    /**
     * Asynchronously creates a new customer.
     */
    public java.util.concurrent.CompletableFuture<Customer> createAsync(CreateCustomerRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return transport.postAsync("/customers", request, Customer.class);
    }

    /**
     * Asynchronously retrieves a customer by unique ID.
     */
    public java.util.concurrent.CompletableFuture<Customer> getByIdAsync(String customerId) {
        validateId(customerId, "customerId");
        return transport.getAsync("/customers/" + encode(customerId), Customer.class);
    }

    /**
     * Asynchronously updates an existing customer.
     */
    public java.util.concurrent.CompletableFuture<Customer> updateAsync(String customerId, UpdateCustomerRequest request) {
        validateId(customerId, "customerId");
        Objects.requireNonNull(request, "request must not be null");
        return transport.putAsync("/customers/" + encode(customerId), request, Customer.class);
    }

    /**
     * Asynchronously deletes a customer by ID.
     */
    public java.util.concurrent.CompletableFuture<Void> deleteAsync(String customerId) {
        validateId(customerId, "customerId");
        return transport.deleteAsync("/customers/" + encode(customerId));
    }

    /**
     * Asynchronously lists customers with default pagination.
     */
    public java.util.concurrent.CompletableFuture<Page<Customer>> listAsync() {
        return listAsync(20, null);
    }

    /**
     * Asynchronously lists customers with pagination parameters.
     */
    public java.util.concurrent.CompletableFuture<Page<Customer>> listAsync(int limit, String startingAfter) {
        StringBuilder path = new StringBuilder("/customers?limit=").append(limit);
        if (startingAfter != null && !startingAfter.isBlank()) {
            path.append("&starting_after=").append(encode(startingAfter));
        }
        return transport.getAsync(path.toString(), new TypeReference<Page<Customer>>() {});
    }

    private void validateId(String id, String paramName) {
        Objects.requireNonNull(id, paramName + " must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException(paramName + " must not be blank");
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
