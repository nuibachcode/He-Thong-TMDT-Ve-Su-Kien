package vn.edu.tmdt.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    public record StockDef(String refType, Long refId, String name, BigDecimal price, int total,
                           int maxPerOrder, Long organizerId) {
    }

    public record StockView(String refType, Long refId, String name, BigDecimal price, int total,
                            int sold, int remaining, Integer maxPerOrder, Long organizerId) {
    }

    public record QueryRequest(String refType, List<Long> ids) {
    }

    public record OrganizerStat(long sold, long quota) {
    }

    private final RestClient http;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${app.services.inventory-url}") String inventoryUrl) {
        this.http = builder.baseUrl(inventoryUrl).build();
    }

    public void ensure(List<StockDef> stock) {
        if (stock == null || stock.isEmpty()) {
            return;
        }
        try {
            http.post().uri("/internal/stock/ensure").body(stock).retrieve()
                    .body(new ParameterizedTypeReference<List<StockView>>() {
                    });
        } catch (RestClientException ex) {
            log.warn("Khong dong bo duoc ton kho: {}", ex.getMessage());
        }
    }

    public Map<Long, Integer> remaining(String refType, List<Long> ids) {
        return query(refType, ids).stream()
                .collect(Collectors.toMap(StockView::refId, StockView::remaining, (a, b) -> a));
    }

    public Map<Long, Integer> sold(String refType, List<Long> ids) {
        return query(refType, ids).stream()
                .collect(Collectors.toMap(StockView::refId, StockView::sold, (a, b) -> a));
    }

    public OrganizerStat organizerStat(Long organizerId) {
        try {
            OrganizerStat stat = http.get()
                    .uri("/internal/stock/organizer/{id}", organizerId)
                    .retrieve().body(OrganizerStat.class);
            return stat == null ? new OrganizerStat(0, 0) : stat;
        } catch (RestClientException ex) {
            log.warn("Khong lay duoc thong ke ton kho: {}", ex.getMessage());
            return new OrganizerStat(0, 0);
        }
    }

    private List<StockView> query(String refType, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        try {
            List<StockView> rows = http.post().uri("/internal/stock/query")
                    .body(new QueryRequest(refType, ids))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StockView>>() {
                    });
            return rows == null ? List.of() : rows;
        } catch (RestClientException ex) {
            log.warn("Khong lay duoc ton kho {}: {}", refType, ex.getMessage());
            return List.of();
        }
    }
}
