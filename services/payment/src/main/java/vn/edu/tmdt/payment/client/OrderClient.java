package vn.edu.tmdt.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import vn.edu.tmdt.payment.common.AppException;

@Service
public class OrderClient {

    public record InternalBooking(String code, Long userId, String status, BigDecimal totalAmount,
                                  Instant expiresAt, Instant confirmedAt) {
    }

    private final RestClient http;
    private final ObjectMapper mapper;

    public OrderClient(RestClient.Builder builder, ObjectMapper mapper,
                       @Value("${app.services.order-url}") String orderUrl) {
        this.http = builder.baseUrl(orderUrl).build();
        this.mapper = mapper;
    }

    public InternalBooking get(String code) {
        try {
            InternalBooking booking = http.get().uri("/internal/bookings/{code}", code)
                    .retrieve().body(InternalBooking.class);
            if (booking == null) {
                throw AppException.notFound("Khong tim thay don hang");
            }
            return booking;
        } catch (RestClientResponseException ex) {
            throw AppException.notFound(readMessage(ex.getResponseBodyAsString(), "Khong tim thay don hang"));
        } catch (RestClientException ex) {
            throw AppException.conflict("Khong ket noi duoc order-service: " + ex.getMessage());
        }
    }

    public InternalBooking confirm(String code, Long userId, String txnRef, String provider) {
        try {
            InternalBooking booking = http.post()
                    .uri(uriBuilder -> uriBuilder.path("/internal/bookings/{code}/confirm")
                            .queryParam("userId", userId)
                            .queryParam("txnRef", txnRef)
                            .queryParam("provider", provider)
                            .build(code))
                    .retrieve().body(InternalBooking.class);
            if (booking == null) {
                throw AppException.conflict("Xac nhan don hang that bai");
            }
            return booking;
        } catch (RestClientResponseException ex) {
            throw AppException.conflict(readMessage(ex.getResponseBodyAsString(),
                    "Khong xac nhan duoc don hang"));
        } catch (RestClientException ex) {
            throw AppException.conflict("Khong ket noi duoc order-service: " + ex.getMessage());
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
