package vn.edu.tmdt.catalog.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("events")
public class Event {

    @Id
    public Long id;
    public Long organizerId;
    public String title;
    public String slug;
    public String category;
    public String description;
    public String coverImageUrl;
    public String venueName;
    public String address;
    public String city;
    public java.time.Instant startAt;
    public java.time.Instant endAt;
    public java.time.Instant createdAt;
    public String status = "PENDING";
    public List<TicketTypeDef> ticketTypes = new ArrayList<>();

    public BigDecimal minPrice() {
        return ticketTypes.stream().map(t -> t.price).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    public int quota() {
        return ticketTypes.stream().mapToInt(t -> t.quota).sum();
    }
}
