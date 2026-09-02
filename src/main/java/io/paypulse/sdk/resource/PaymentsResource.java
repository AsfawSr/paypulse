package io.paypulse.sdk.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import io.paypulse.sdk.http.HttpTransport;
import io.paypulse.sdk.model.common.Page;
import io.paypulse.sdk.model.payment.ChargeRequest;
import io.paypulse.sdk.model.payment.Payment;
import io.paypulse.sdk.model.payment.RefundRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Resource service providing operations for creating charges, issuing refunds, and querying payments.
 */
public class PaymentsResource {

    private final HttpTransport transport;

    public PaymentsResource(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
    }

    /**
     * Charges a customer or payment source.
     *
     * @param request the charge details
     * @return the resulting Payment record
     */
    public Payment charge(ChargeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return transport.post("/payments", request, Payment.class, request.idempotencyKey());
    }

    /**
     * Retrieves a payment by its unique ID.
     *
     * @param paymentId the payment identifier
     * @return the Payment record
     */
    public Payment getById(String paymentId) {
        validateId(paymentId, "paymentId");
        return transport.get("/payments/" + encode(paymentId), Payment.class);
    }

    /**
     * Issues a full or partial refund for a payment.
     *
     * @param paymentId the payment identifier
     * @param request the refund details
     * @return the updated Payment record reflecting the refund
     */
    public Payment refund(String paymentId, RefundRequest request) {
        validateId(paymentId, "paymentId");
        String idempotencyKey = (request != null) ? request.idempotencyKey() : null;
        return transport.post("/payments/" + encode(paymentId) + "/refund", request, Payment.class, idempotencyKey);
    }

    /**
     * Lists payments with default pagination.
     *
     * @return a Page of Payment entities
     */
    public Page<Payment> list() {
        return list(20, null, null);
    }

    /**
     * Lists payments with filtering and pagination parameters.
     *
     * @param limit maximum number of items to return
     * @param customerId optional filter to only retrieve payments for a specific customer
     * @param startingAfter cursor pointer for pagination
     * @return a Page of Payment entities
     */
    public Page<Payment> list(int limit, String customerId, String startingAfter) {
        StringBuilder path = new StringBuilder("/payments?limit=").append(limit);
        if (customerId != null && !customerId.isBlank()) {
            path.append("&customer_id=").append(encode(customerId));
        }
        if (startingAfter != null && !startingAfter.isBlank()) {
            path.append("&starting_after=").append(encode(startingAfter));
        }
        return transport.get(path.toString(), new TypeReference<Page<Payment>>() {});
    }

    // ==========================================
    // Asynchronous API Methods
    // ==========================================

    /**
     * Asynchronously charges a customer or payment source.
     */
    public java.util.concurrent.CompletableFuture<Payment> chargeAsync(ChargeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return transport.postAsync("/payments", request, Payment.class, request.idempotencyKey());
    }

    /**
     * Asynchronously retrieves a payment by unique ID.
     */
    public java.util.concurrent.CompletableFuture<Payment> getByIdAsync(String paymentId) {
        validateId(paymentId, "paymentId");
        return transport.getAsync("/payments/" + encode(paymentId), Payment.class);
    }

    /**
     * Asynchronously refunds a payment.
     */
    public java.util.concurrent.CompletableFuture<Payment> refundAsync(String paymentId, RefundRequest request) {
        validateId(paymentId, "paymentId");
        String idempotencyKey = (request != null) ? request.idempotencyKey() : null;
        return transport.postAsync("/payments/" + encode(paymentId) + "/refund", request, Payment.class, idempotencyKey);
    }

    /**
     * Asynchronously lists payments with default pagination.
     */
    public java.util.concurrent.CompletableFuture<Page<Payment>> listAsync() {
        return listAsync(20, null, null);
    }

    /**
     * Asynchronously lists payments with filtering and pagination parameters.
     */
    public java.util.concurrent.CompletableFuture<Page<Payment>> listAsync(int limit, String customerId, String startingAfter) {
        StringBuilder path = new StringBuilder("/payments?limit=").append(limit);
        if (customerId != null && !customerId.isBlank()) {
            path.append("&customer_id=").append(encode(customerId));
        }
        if (startingAfter != null && !startingAfter.isBlank()) {
            path.append("&starting_after=").append(encode(startingAfter));
        }
        return transport.getAsync(path.toString(), new TypeReference<Page<Payment>>() {});
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
