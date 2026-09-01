package io.paypulse.sdk.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Generic paginated list response returned by list endpoints.
 *
 * @param <T> The element type contained in the page
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Page<T>(
        @JsonProperty("data") List<T> data,
        @JsonProperty("hasMore") boolean hasMore,
        @JsonProperty("nextCursor") String nextCursor,
        @JsonProperty("totalCount") int totalCount
) {
    public Page {
        if (data == null) {
            data = Collections.emptyList();
        }
    }
}
