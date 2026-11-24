package vn.edu.tmdt.catalog.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import vn.edu.tmdt.catalog.common.AppException;
import vn.edu.tmdt.catalog.config.RabbitConfig;
import vn.edu.tmdt.catalog.domain.Event;
import vn.edu.tmdt.catalog.domain.ScheduleDef;
import vn.edu.tmdt.catalog.domain.TicketTypeDef;
import vn.edu.tmdt.catalog.domain.Tour;
import vn.edu.tmdt.catalog.repository.EventRepository;
import vn.edu.tmdt.catalog.repository.TourRepository;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public record EventCard(Long id, String title, String slug, String category, String coverImageUrl,
                            Instant startAt, String venueName, String city, BigDecimal minPrice) {
    }

    public record TicketView(Long id, String name, BigDecimal price, int remaining, int maxPerOrder) {
    }

    public record EventDetail(Long id, String title, String slug, String category, String coverImageUrl,
                              Instant startAt, String venueName, String city, BigDecimal minPrice,
                              Instant endAt, String description, String address,
                              List<TicketView> tickets) {
    }

    public record TourCard(Long id, String title, String slug, String destination, String departureCity,
                           int durationDays, int durationNights, BigDecimal basePrice, String coverImageUrl) {
    }

    public record ScheduleView(Long id, LocalDate departureDate, LocalDate returnDate,
                               BigDecimal price, int remaining) {
    }

    public record TourDetail(Long id, String title, String slug, String destination, String departureCity,
                             int durationDays, int durationNights, BigDecimal basePrice, String coverImageUrl,
                             String description, List<String> highlights, List<String> included,
                             List<String> excluded, List<ScheduleView> schedules) {
    }

    public record OrgEvent(Long id, String title, String slug, String status, String category,
                           Instant startAt, int sold, int quota) {
    }

    public record PendingView(Long id, String title, String type, String status) {
    }

    public record CreatedTour(Long id, String title, String status) {
    }

    public record TicketInput(String name, BigDecimal price, Integer quota, Integer maxPerOrder) {
    }

    public record EventInput(String title, String category, String description, String startAt, String endAt,
                             String coverImageUrl, String venueName, String address, String city,
                             List<TicketInput> tickets) {
    }

    public record ScheduleInput(LocalDate departureDate, LocalDate returnDate, BigDecimal price,
                                Integer seatsTotal) {
    }

    public record TourInput(String title, String destination, String departureCity, Integer durationDays,
                            Integer durationNights, BigDecimal basePrice, String description,
                            String highlights, String included, String excluded, String coverImageUrl,
                            List<ScheduleInput> schedules) {
    }

    public record ItemMeta(String refType, Long refId, String name, BigDecimal price, Integer maxPerOrder,
                           Long organizerId, Long contentId, String contentTitle, String status) {
    }

    private final EventRepository events;
    private final TourRepository tours;
    private final SequenceService sequence;
    private final InventoryClient inventory;
    private final RabbitTemplate rabbit;

    public CatalogService(EventRepository events, TourRepository tours, SequenceService sequence,
                          InventoryClient inventory, RabbitTemplate rabbit) {
        this.events = events;
        this.tours = tours;
        this.sequence = sequence;
        this.inventory = inventory;
        this.rabbit = rabbit;
    }

    // ---------- PUBLIC ----------

    public List<EventCard> listEvents(String q, String category) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        String cat = category == null ? "" : category.trim().toLowerCase();
        return events.findByStatusOrderByStartAtAsc("PUBLISHED").stream()
                .filter(event -> needle.isBlank() || event.title.toLowerCase().contains(needle))
                .filter(event -> cat.isBlank() || cat.equalsIgnoreCase(event.category))
                .map(this::eventCard)
                .toList();
    }

    public EventDetail eventDetail(String slug) {
        Event event = events.findBySlug(slug)
                .filter(value -> "PUBLISHED".equals(value.status))
                .orElseThrow(() -> AppException.notFound("Khong tim thay su kien"));
        ensureEventStock(event);
        Map<Long, Integer> remaining = inventory.remaining("TICKET",
                event.ticketTypes.stream().map(t -> t.id).toList());
        List<TicketView> tickets = event.ticketTypes.stream()
                .map(ticket -> new TicketView(ticket.id, ticket.name, ticket.price,
                        remaining.getOrDefault(ticket.id, ticket.quota), ticket.maxPerOrder))
                .toList();
        return new EventDetail(event.id, event.title, event.slug, event.category, event.coverImageUrl,
                event.startAt, event.venueName, event.city, event.minPrice(), event.endAt,
                event.description, event.address, tickets);
    }

    public List<TourCard> listTours(String q, String destination) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        String dest = destination == null ? "" : destination.trim().toLowerCase();
        return tours.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .filter(tour -> needle.isBlank() || tour.title.toLowerCase().contains(needle))
                .filter(tour -> dest.isBlank() || tour.destination.toLowerCase().contains(dest))
                .map(this::tourCard)
                .toList();
    }

    public TourDetail tourDetail(String slug) {
        Tour tour = tours.findBySlug(slug)
                .filter(value -> "PUBLISHED".equals(value.status))
                .orElseThrow(() -> AppException.notFound("Khong tim thay tour"));
        ensureTourStock(tour);
        Map<Long, Integer> remaining = inventory.remaining("SCHEDULE",
                tour.schedules.stream().map(s -> s.id).toList());
        List<ScheduleView> schedules = tour.schedules.stream()
                .map(schedule -> new ScheduleView(schedule.id, schedule.departureDate, schedule.returnDate,
                        schedule.price, remaining.getOrDefault(schedule.id, schedule.seatsTotal)))
                .toList();
        return new TourDetail(tour.id, tour.title, tour.slug, tour.destination, tour.departureCity,
                tour.durationDays, tour.durationNights, tour.basePrice, tour.coverImageUrl,
                tour.description, tour.highlights, tour.included, tour.excluded, schedules);
    }

    // ---------- ORGANIZER ----------

    public List<OrgEvent> organizerEvents(Long organizerId) {
        return events.findByOrganizerIdOrderByStartAtDesc(organizerId).stream()
                .map(event -> {
                    ensureEventStock(event);
                    Map<Long, Integer> sold = inventory.sold("TICKET",
                            event.ticketTypes.stream().map(t -> t.id).toList());
                    int totalSold = event.ticketTypes.stream()
                            .mapToInt(t -> sold.getOrDefault(t.id, 0)).sum();
                    return new OrgEvent(event.id, event.title, event.slug, event.status, event.category,
                            event.startAt, totalSold, event.quota());
                })
                .toList();
    }

    public OrgEvent createEvent(Long organizerId, EventInput input) {
        if (input.title() == null || input.title().isBlank()) {
            throw AppException.badRequest("Tieu de khong duoc de trong");
        }
        if (input.tickets() == null || input.tickets().isEmpty()) {
            throw AppException.badRequest("Phai co it nhat mot loai ve");
        }
        Event event = new Event();
        event.id = sequence.next("event");
        event.organizerId = organizerId;
        event.title = input.title().trim();
        event.slug = slugify(event.title) + "-" + event.id;
        event.category = input.category() == null ? "other" : input.category();
        event.description = input.description();
        event.coverImageUrl = input.coverImageUrl();
        event.venueName = blank(input.venueName()) ? "TicketGo Venue" : input.venueName();
        event.address = blank(input.address()) ? "Dang cap nhat" : input.address();
        event.city = blank(input.city()) ? "TP. Ho Chi Minh" : input.city();
        event.startAt = parseInstant(input.startAt(), 20, 0);
        event.endAt = input.endAt() == null || input.endAt().isBlank()
                ? event.startAt.plusSeconds(2 * 3600)
                : parseInstant(input.endAt(), 22, 0);
        event.createdAt = Instant.now();
        event.status = "PENDING";
        for (TicketInput ticket : input.tickets()) {
            TicketTypeDef def = new TicketTypeDef();
            def.id = sequence.next("ticket");
            def.name = ticket.name() == null ? "Standard" : ticket.name();
            def.price = ticket.price() == null ? BigDecimal.ZERO : ticket.price();
            def.quota = ticket.quota() == null ? 0 : ticket.quota();
            def.maxPerOrder = ticket.maxPerOrder() == null ? 4 : ticket.maxPerOrder();
            event.ticketTypes.add(def);
        }
        events.save(event);
        ensureEventStock(event);
        publishChanged("EVENT", event.id);
        return new OrgEvent(event.id, event.title, event.slug, event.status, event.category,
                event.startAt, 0, event.quota());
    }

    public CreatedTour createTour(Long organizerId, TourInput input) {
        if (input.title() == null || input.title().isBlank()) {
            throw AppException.badRequest("Tieu de tour khong duoc de trong");
        }
        Tour tour = new Tour();
        tour.id = sequence.next("tour");
        tour.organizerId = organizerId;
        tour.title = input.title().trim();
        tour.slug = slugify(tour.title) + "-" + tour.id;
        tour.destination = input.destination() == null ? "" : input.destination();
        tour.departureCity = input.departureCity() == null ? "" : input.departureCity();
        tour.durationDays = input.durationDays() == null ? 2 : input.durationDays();
        tour.durationNights = input.durationNights() == null ? 1 : input.durationNights();
        tour.basePrice = input.basePrice() == null ? BigDecimal.ZERO : input.basePrice();
        tour.description = input.description();
        tour.coverImageUrl = input.coverImageUrl();
        tour.highlights = parseLines(input.highlights());
        tour.included = parseLines(input.included());
        tour.excluded = parseLines(input.excluded());
        tour.createdAt = Instant.now();
        tour.status = "PENDING";
        if (input.schedules() != null) {
            for (ScheduleInput schedule : input.schedules()) {
                ScheduleDef def = new ScheduleDef();
                def.id = sequence.next("schedule");
                def.departureDate = schedule.departureDate();
                def.returnDate = schedule.returnDate() == null && schedule.departureDate() != null
                        ? schedule.departureDate().plusDays(Math.max(1, tour.durationDays - 1))
                        : schedule.returnDate();
                def.price = schedule.price() == null ? tour.basePrice : schedule.price();
                def.seatsTotal = schedule.seatsTotal() == null ? 0 : schedule.seatsTotal();
                tour.schedules.add(def);
            }
        }
        tours.save(tour);
        ensureTourStock(tour);
        publishChanged("TOUR", tour.id);
        return new CreatedTour(tour.id, tour.title, tour.status);
    }

    // ---------- ADMIN ----------

    public List<PendingView> pending() {
        List<PendingView> result = new ArrayList<>();
        events.findByStatusOrderByStartAtAsc("PENDING")
                .forEach(event -> result.add(new PendingView(event.id, event.title, "EVENT", event.status)));
        tours.findByStatusOrderByCreatedAtDesc("PENDING")
                .forEach(tour -> result.add(new PendingView(tour.id, tour.title, "TOUR", tour.status)));
        return result;
    }

    public String setContentStatus(String type, Long id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("PUBLISHED", "REJECTED", "PENDING").contains(normalized)) {
            throw AppException.badRequest("Trang thai khong hop le");
        }
        if ("EVENT".equalsIgnoreCase(type)) {
            Event event = events.findById(id).orElseThrow(() -> AppException.notFound("Khong tim thay su kien"));
            event.status = normalized;
            events.save(event);
            ensureEventStock(event);
        } else if ("TOUR".equalsIgnoreCase(type)) {
            Tour tour = tours.findById(id).orElseThrow(() -> AppException.notFound("Khong tim thay tour"));
            tour.status = normalized;
            tours.save(tour);
            ensureTourStock(tour);
        } else {
            throw AppException.badRequest("Loai noi dung khong hop le");
        }
        publishChanged(type.toUpperCase(), id);
        return normalized;
    }

    // ---------- INTERNAL ----------

    public List<ItemMeta> items(String refType, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ItemMeta> result = new ArrayList<>();
        if ("TICKET".equalsIgnoreCase(refType)) {
            for (Event event : events.findAll()) {
                boolean matched = event.ticketTypes.stream().anyMatch(ticket -> ids.contains(ticket.id));
                if (matched) {
                    ensureEventStock(event);
                }
                for (TicketTypeDef ticket : event.ticketTypes) {
                    if (ids.contains(ticket.id)) {
                        result.add(new ItemMeta("TICKET", ticket.id, event.title + " - " + ticket.name,
                                ticket.price, ticket.maxPerOrder, event.organizerId, event.id, event.title,
                                event.status));
                    }
                }
            }
        } else if ("SCHEDULE".equalsIgnoreCase(refType)) {
            for (Tour tour : tours.findAll()) {
                boolean matched = tour.schedules.stream().anyMatch(schedule -> ids.contains(schedule.id));
                if (matched) {
                    ensureTourStock(tour);
                }
                for (ScheduleDef schedule : tour.schedules) {
                    if (ids.contains(schedule.id)) {
                        result.add(new ItemMeta("SCHEDULE", schedule.id,
                                tour.title + " - " + schedule.departureDate, schedule.price, 10,
                                tour.organizerId, tour.id, tour.title, tour.status));
                    }
                }
            }
        }
        return result;
    }

    public boolean ownedBy(String refType, Long refId, Long organizerId) {
        if ("TICKET".equalsIgnoreCase(refType)) {
            return events.findAll().stream().anyMatch(event -> event.organizerId.equals(organizerId)
                    && event.ticketTypes.stream().anyMatch(t -> t.id.equals(refId)));
        }
        if ("SCHEDULE".equalsIgnoreCase(refType)) {
            return tours.findAll().stream().anyMatch(tour -> tour.organizerId.equals(organizerId)
                    && tour.schedules.stream().anyMatch(s -> s.id.equals(refId)));
        }
        return false;
    }

    public long organizerPendingCount(Long organizerId) {
        return organizerPendingCounts(organizerId).values().stream().mapToLong(Long::longValue).sum();
    }

    public Map<String, Long> organizerPendingCounts(Long organizerId) {
        long pendingEvents = events.findByOrganizerIdOrderByStartAtDesc(organizerId).stream()
                .filter(event -> "PENDING".equals(event.status)).count();
        long pendingTours = tours.findByOrganizerIdOrderByCreatedAtDesc(organizerId).stream()
                .filter(tour -> "PENDING".equals(tour.status)).count();
        return Map.of("pendingEvents", pendingEvents, "pendingTours", pendingTours);
    }

    public Map<String, Long> counts() {
        return Map.of("events", events.count(), "tours", tours.count(),
                "pendingEvents", events.countByStatus("PENDING"),
                "pendingTours", tours.countByStatus("PENDING"));
    }

    // ---------- HELPERS ----------

    private void ensureEventStock(Event event) {
        inventory.ensure(event.ticketTypes.stream()
                .map(ticket -> new InventoryClient.StockDef("TICKET", ticket.id,
                        event.title + " - " + ticket.name, ticket.price, ticket.quota,
                        ticket.maxPerOrder, event.organizerId))
                .toList());
    }

    private void ensureTourStock(Tour tour) {
        inventory.ensure(tour.schedules.stream()
                .map(schedule -> new InventoryClient.StockDef("SCHEDULE", schedule.id,
                        tour.title + " - " + schedule.departureDate, schedule.price, schedule.seatsTotal,
                        10, tour.organizerId))
                .toList());
    }

    private EventCard eventCard(Event event) {
        return new EventCard(event.id, event.title, event.slug, event.category, event.coverImageUrl,
                event.startAt, event.venueName, event.city, event.minPrice());
    }

    private TourCard tourCard(Tour tour) {
        return new TourCard(tour.id, tour.title, tour.slug, tour.destination, tour.departureCity,
                tour.durationDays, tour.durationNights, tour.basePrice, tour.coverImageUrl);
    }

    private void publishChanged(String type, Long id) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, "catalog.content.changed",
                    type + ":" + id);
        } catch (RuntimeException ex) {
            log.warn("Khong gui duoc su kien catalog: {}", ex.getMessage());
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static List<String> parseLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[\\n,;]+"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private static Instant parseInstant(String value, int hour, int minute) {
        if (value == null || value.isBlank()) {
            return LocalDate.now().plusDays(30).atTime(hour, minute).atZone(ZONE).toInstant();
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (Exception ignored) {
            // fall through
        }
        try {
            return LocalDateTime.parse(value).atZone(ZONE).toInstant();
        } catch (Exception ex) {
            throw AppException.badRequest("Thoi gian khong hop le: " + value);
        }
    }

    public static String slugify(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value.trim(),
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
