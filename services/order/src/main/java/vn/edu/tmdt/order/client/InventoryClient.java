package vn.edu.tmdt.order.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import vn.edu.tmdt.order.common.AppException;

@Service
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    public record HoldItem(String refType, Long refId, int quantity) {
    }

    public record HoldRequest(String bookingCode, Instant expiresAt, List<HoldItem> items) {
    }

    public record HoldLine(String refType, Long refId, String itemName, int quantity,
                           BigDecimal unitPrice, BigDecimal amount, Long organizerId) {
    }

    public record HoldResult(String bookingCode, List<HoldLine> items, BigDecimal total) {
    }

    public record TicketView(Long ticketId, String itemName, int quantity, String qrPngBase64,
                             Instant checkedInAt) {
    }

    public record OrganizerStat(long sold, long quota) {
    }

    private final RestClient http;
    private final ObjectMapper mapper;

    public InventoryClient(RestClient.Builder builder, ObjectMapper mapper,
                           @Value("${app.services.inventory-url}") String inventoryUrl) {
        this.http = builder.baseUrl(inventoryUrl).build();
        this.mapper = mapper;
    }

    public HoldResult hold(String bookingCode, Instant expiresAt, List<HoldItem> items) {
        try {
            return http.post().uri("/internal/holds")
                    .body(new HoldRequest(bookingCode, expiresAt, items))
                    .retrieve().body(HoldResult.class);
        } catch (RestClientResponseException ex) {
            throw AppException.conflict(readMessage(ex.getResponseBodyAsString(),
                    "Khong du ve cho don hang"));
        } catch (RestClientException ex) {
            throw AppException.conflict("Khong ket noi duoc kho ve: " + ex.getMessage());
        }
    }

    public void confirm(String bookingCode) {
        try {
            http.post().uri("/internal/holds/{code}/confirm", bookingCode).retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw AppException.conflict(readMessage(ex.getResponseBodyAsString(),
                    "Khong xac nhan duoc giu cho"));
        } catch (RestClientException ex) {
            throw AppException.conflict("Khong ket noi duoc kho ve: " + ex.getMessage());
        }
    }

    public void release(String bookingCode) {
        try {
            http.post().uri("/internal/holds/{code}/release", bookingCode).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Khong release duoc hold {}: {}", bookingCode, ex.getMessage());
        }
    }

    public List<TicketView> tickets(String bookingCode) {
        try {
            List<TicketView> rows = http.get().uri("/internal/tickets/{code}", bookingCode)
                    .retrieve().body(new ParameterizedTypeReference<List<TicketView>>() {
                    });
            return rows == null ? List.of() : rows;
        } catch (RestClientException ex) {
            return List.of();
        }
    }

    public OrganizerStat organizerStat(Long organizerId) {
        try {
            OrganizerStat stat = http.get().uri("/internal/stock/organizer/{id}", organizerId)
                    .retrieve().body(OrganizerStat.class);
            return stat == null ? new OrganizerStat(0, 0) : stat;
        } catch (RestClientException ex) {
            return new OrganizerStat(0, 0);
        }
    }

    private String readMessage(String body, String fallback) {
        try {
            JsonNode node = mapper.readTree(body);
            String message = node.path("message").asText("");
            return message.isBlank() ? fallback : message;
        } catch (Exception ex) {
            return fallback;
        }
    }
}
