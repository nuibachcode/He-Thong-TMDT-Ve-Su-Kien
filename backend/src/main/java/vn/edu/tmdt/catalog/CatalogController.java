package vn.edu.tmdt.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.common.ApiResponse;
import vn.edu.tmdt.common.exception.AppException;

@RestController
@RequestMapping("/api/public")
public class CatalogController {

    private final EventRepository events;
    private final TicketTypeRepository tickets;
    private final TourRepository tours;
    private final TourScheduleRepository schedules;
    private final VenueRepository venues;

    public CatalogController(EventRepository events, TicketTypeRepository tickets,
                             TourRepository tours, TourScheduleRepository schedules,
                             VenueRepository venues) {
        this.events = events;
        this.tickets = tickets;
        this.tours = tours;
        this.schedules = schedules;
        this.venues = venues;
    }

    // ---------- SU KIEN ----------

    @GetMapping("/events")
    public ApiResponse<List<EventCard>> listEvents(@RequestParam(required = false) String q,
                                                   @RequestParam(required = false) String category) {
        return ApiResponse.ok(events.search(blank(q), blank(category)).stream().map(this::toCard).toList());
    }

    @GetMapping("/events/{slug}")
    public ApiResponse<EventDetail> eventDetail(@PathVariable String slug) {
        Event e = events.findBySlug(slug).orElseThrow(() -> AppException.notFound("Khong tim thay su kien"));
        Venue v = e.venueId == null ? null : venues.findById(e.venueId).orElse(null);
        List<TicketView> t = tickets.findByEventId(e.id).stream().map(x -> new TicketView(
                x.id, x.name, x.price, x.remaining(), x.maxPerOrder)).toList();
        EventCard card = toCard(e);
        return ApiResponse.ok(new EventDetail(card.id(), card.title(), card.slug(), card.category(),
                card.coverImageUrl(), e.startAt, e.endAt, e.description, card.venueName(), card.city(),
                v == null ? null : v.address, card.minPrice(), t));
    }

    // ---------- TOUR ----------

    @GetMapping("/tours")
    public ApiResponse<List<TourCard>> listTours(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String destination) {
        return ApiResponse.ok(tours.search(blank(q), blank(destination)).stream().map(this::toCard).toList());
    }

    @GetMapping("/tours/{slug}")
    public ApiResponse<TourDetail> tourDetail(@PathVariable String slug) {
        Tour t = tours.findBySlug(slug).orElseThrow(() -> AppException.notFound("Khong tim thay tour"));
        List<ScheduleView> s = schedules.findByTourIdOrderByDepartureDateAsc(t.id).stream()
                .map(x -> new ScheduleView(x.id, x.departureDate, x.returnDate, x.price, x.remaining()))
                .toList();
        TourCard card = toCard(t);
        return ApiResponse.ok(new TourDetail(card.id(), card.title(), card.slug(), card.destination(),
                card.departureCity(), card.durationDays(), card.durationNights(), card.basePrice(),
                card.coverImageUrl(), t.description, split(t.highlights), split(t.included), split(t.excluded), s));
    }

    // ---------- MAP ----------

    private EventCard toCard(Event e) {
        Venue v = e.venueId == null ? null : venues.findById(e.venueId).orElse(null);
        BigDecimal min = tickets.findByEventId(e.id).stream().map(x -> x.price)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return new EventCard(e.id, e.title, e.slug, e.category, e.coverImageUrl, e.startAt,
                v == null ? null : v.name, v == null ? null : v.city, min);
    }

    private TourCard toCard(Tour t) {
        return new TourCard(t.id, t.title, t.slug, t.destination, t.departureCity,
                t.durationDays, t.durationNights, t.basePrice, t.coverImageUrl);
    }

    private String blank(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split("\\r?\\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public record EventCard(Long id, String title, String slug, String category, String coverImageUrl,
                            Instant startAt, String venueName, String city, BigDecimal minPrice) {
    }

    public record EventDetail(Long id, String title, String slug, String category, String coverImageUrl,
                              Instant startAt, Instant endAt, String description, String venueName,
                              String city, String address, BigDecimal minPrice, List<TicketView> tickets) {
    }

    public record TicketView(Long id, String name, BigDecimal price, int remaining, int maxPerOrder) {
    }

    public record TourCard(Long id, String title, String slug, String destination, String departureCity,
                           Integer durationDays, Integer durationNights, BigDecimal basePrice,
                           String coverImageUrl) {
    }

    public record TourDetail(Long id, String title, String slug, String destination, String departureCity,
                             Integer durationDays, Integer durationNights, BigDecimal basePrice,
                             String coverImageUrl, String description, List<String> highlights,
                             List<String> included, List<String> excluded, List<ScheduleView> schedules) {
    }

    public record ScheduleView(Long id, LocalDate departureDate, LocalDate returnDate,
                               BigDecimal price, int remaining) {
    }
}
