package io.paypulse.sdk.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.paypulse.sdk.config.PayPulseConfig;
import io.paypulse.sdk.exception.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Core HTTP transport layer that executes requests, manages serialization, and maps HTTP errors to typed SDK exceptions.
 */
public class HttpTransport {

    private static final String SDK_VERSION = "1.0.0";
    private static final String USER_AGENT = "PayPulse-Java-SDK/" + SDK_VERSION;

    private final PayPulseConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpTransport(PayPulseConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .build());
    }

    public HttpTransport(PayPulseConfig config, HttpClient httpClient) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = createDefaultObjectMapper();
    }

    public static ObjectMapper createDefaultObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public <T> T get(String path, Class<T> responseType) {
        HttpRequest request = newRequestBuilder(path)
                .GET()
                .build();
        return execute(request, responseType);
    }

    public <T> T get(String path, TypeReference<T> typeRef) {
        HttpRequest request = newRequestBuilder(path)
                .GET()
                .build();
        return execute(request, typeRef);
    }

    public <T> T post(String path, Object requestBody, Class<T> responseType) {
        return post(path, requestBody, responseType, null);
    }

    public <T> T post(String path, Object requestBody, Class<T> responseType, String idempotencyKey) {
        HttpRequest.Builder builder = newRequestBuilder(path);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.header("Idempotency-Key", idempotencyKey);
        }

        String jsonBody = serialize(requestBody);
        builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        return execute(builder.build(), responseType);
    }

    public <T> T put(String path, Object requestBody, Class<T> responseType) {
        String jsonBody = serialize(requestBody);
        HttpRequest request = newRequestBuilder(path)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return execute(request, responseType);
    }

    public void delete(String path) {
        HttpRequest request = newRequestBuilder(path)
                .DELETE()
                .build();
        execute(request, Void.class);
    }

    // ==========================================
    // Asynchronous (Non-blocking) API Methods
    // ==========================================

    public <T> java.util.concurrent.CompletableFuture<T> getAsync(String path, Class<T> responseType) {
        HttpRequest request = newRequestBuilder(path).GET().build();
        return executeAsync(request, body -> deserialize(body, responseType));
    }

    public <T> java.util.concurrent.CompletableFuture<T> getAsync(String path, TypeReference<T> typeRef) {
        HttpRequest request = newRequestBuilder(path).GET().build();
        return executeAsync(request, body -> deserialize(body, typeRef));
    }

    public <T> java.util.concurrent.CompletableFuture<T> postAsync(String path, Object requestBody, Class<T> responseType) {
        return postAsync(path, requestBody, responseType, null);
    }

    public <T> java.util.concurrent.CompletableFuture<T> postAsync(String path, Object requestBody, Class<T> responseType, String idempotencyKey) {
        HttpRequest.Builder builder = newRequestBuilder(path);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.header("Idempotency-Key", idempotencyKey);
        }
        String jsonBody = serialize(requestBody);
        builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        return executeAsync(builder.build(), body -> deserialize(body, responseType));
    }

    public <T> java.util.concurrent.CompletableFuture<T> putAsync(String path, Object requestBody, Class<T> responseType) {
        String jsonBody = serialize(requestBody);
        HttpRequest request = newRequestBuilder(path)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return executeAsync(request, body -> deserialize(body, responseType));
    }

    public java.util.concurrent.CompletableFuture<Void> deleteAsync(String path) {
        HttpRequest request = newRequestBuilder(path).DELETE().build();
        return executeAsync(request, body -> null);
    }

    private HttpRequest.Builder newRequestBuilder(String path) {
        String fullUrl = buildFullUrl(path);
        return HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(config.timeout())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private String buildFullUrl(String path) {
        String base = config.baseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private String serialize(Object object) {
        if (object == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new PayPulseException("Failed to serialize request body to JSON", e);
        }
    }

    private <T> T deserialize(String body, Class<T> responseType) {
        if (responseType == Void.class || body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, responseType);
        } catch (Exception e) {
            throw new PayPulseException("Failed to deserialize response JSON", e);
        }
    }

    private <T> T deserialize(String body, TypeReference<T> typeRef) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(body, typeRef);
        } catch (Exception e) {
            throw new PayPulseException("Failed to deserialize response JSON", e);
        }
    }

    private <T> java.util.concurrent.CompletableFuture<T> executeAsync(HttpRequest request, java.util.function.Function<String, T> deserializer) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    handleErrorStatus(response);
                    if (response.statusCode() == 204 || response.body().isBlank()) {
                        return null;
                    }
                    return deserializer.apply(response.body());
                });
    }

    private <T> T execute(HttpRequest request, Class<T> responseType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            handleErrorStatus(response);

            if (responseType == Void.class || response.statusCode() == 204 || response.body().isBlank()) {
                return null;
            }

            return objectMapper.readValue(response.body(), responseType);
        } catch (IOException e) {
            String errorDetail = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getSimpleName();
            throw new NetworkException("I/O error during API request: " + errorDetail, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorDetail = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getSimpleName();
            throw new NetworkException("API request was interrupted: " + errorDetail, e);
        }
    }

    private <T> T execute(HttpRequest request, TypeReference<T> typeRef) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            handleErrorStatus(response);

            if (response.statusCode() == 204 || response.body().isBlank()) {
                return null;
            }

            return objectMapper.readValue(response.body(), typeRef);
        } catch (IOException e) {
            String errorDetail = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getSimpleName();
            throw new NetworkException("I/O error during API request: " + errorDetail, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorDetail = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : e.getClass().getSimpleName();
            throw new NetworkException("API request was interrupted: " + errorDetail, e);
        }
    }

    private void handleErrorStatus(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return; // Success
        }

        ErrorResponse errorResponse = null;
        try {
            if (response.body() != null && !response.body().isBlank()) {
                errorResponse = objectMapper.readValue(response.body(), ErrorResponse.class);
            }
        } catch (Exception ignored) {
            // Raw response body could not be parsed as ErrorResponse
        }

        String message = (errorResponse != null) ? errorResponse.resolveMessage() : "HTTP Error " + status;
        String code = (errorResponse != null) ? errorResponse.code() : "HTTP_" + status;

        switch (status) {
            case 401, 403 -> throw new AuthenticationException(message, status, code);
            case 404 -> throw new ResourceNotFoundException(message, status, code);
            case 400, 422 -> throw new ValidationException(message, status, code);
            case 429 -> {
                Integer retryAfter = response.headers()
                        .firstValue("Retry-After")
                        .map(this::parseIntegerSafe)
                        .orElse(null);
                throw new RateLimitException(message, status, code, retryAfter);
            }
            default -> {
                if (status >= 500) {
                    throw new ApiServerException(message, status, code);
                }
                throw new PayPulseException(message, status, code);
            }
        }
    }

    private Integer parseIntegerSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
