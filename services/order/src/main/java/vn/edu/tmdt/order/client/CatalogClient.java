package vn.edu.tmdt.order.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final RestClient http;

    public CatalogClient(RestClient.Builder builder,
                         @Value("${app.services.catalog-url}") String catalogUrl) {
        this.http = builder.baseUrl(catalogUrl).build();
    }

    public record ItemMeta(String refType, Long refId, String name, BigDecimal price, Integer maxPerOrder,
                           Long organizerId, Long contentId, String contentTitle, String status) {
    }

    public List<ItemMeta> items(String refType, List<Long> ids) {
        try {
            List<ItemMeta> rows = http.post().uri("/internal/items")
                    .body(java.util.Map.of("refType", refType, "ids", ids))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ItemMeta>>() {
                    });
            return rows == null ? List.of() : rows;
        } catch (RestClientException ex) {
            log.warn("Khong lay duoc metadata {}: {}", refType, ex.getMessage());
            return List.of();
        }
    }

    public Map<String, Long> pendingCounts(Long organizerId) {
        try {
            Map<String, Long> body = http.get()
                    .uri("/internal/organizer/pending-count?organizerId={id}", organizerId)
                    .retrieve().body(new ParameterizedTypeReference<Map<String, Long>>() {
                    });
            return body == null ? Map.of() : body;
        } catch (RestClientException ex) {
            log.warn("Khong lay duoc pending count: {}", ex.getMessage());
            return Map.of();
        }
    }

    public long pending(Long organizerId) {
        return pendingCounts(organizerId).values().stream().mapToLong(Long::longValue).sum();
    }

    public Map<String, Long> counts() {
        try {
            Map<String, Long> body = http.get().uri("/internal/stats/counts").retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Long>>() {
                    });
            return body == null ? Map.of() : body;
        } catch (RestClientException ex) {
            log.warn("Khong lay duoc catalog counts: {}", ex.getMessage());
            return Map.of();
        }
    }
}
