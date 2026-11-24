package vn.edu.tmdt.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tours")
public class Tour {

    @Id
    public Long id;
    public Long organizerId;
    public String title;
    public String slug;
    public String destination;
    public String departureCity;
    public int durationDays;
    public int durationNights;
    public BigDecimal basePrice;
    public String coverImageUrl;
    public String description;
    public List<String> highlights = new ArrayList<>();
    public List<String> included = new ArrayList<>();
    public List<String> excluded = new ArrayList<>();
    public Instant createdAt;
    public String status = "PENDING";
    public List<ScheduleDef> schedules = new ArrayList<>();

    public int quota() {
        return schedules.stream().mapToInt(s -> s.seatsTotal).sum();
    }

    public BigDecimal minSchedulePrice() {
        return schedules.stream().map(s -> s.price).min(BigDecimal::compareTo).orElse(basePrice);
    }

    public LocalDate firstDeparture() {
        return schedules.isEmpty() ? null : schedules.get(0).departureDate;
    }
}
