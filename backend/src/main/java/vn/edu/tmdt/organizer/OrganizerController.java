package vn.edu.tmdt.organizer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.ai.IngestService;
import vn.edu.tmdt.ai.LlmClient;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.booking.Booking;
import vn.edu.tmdt.booking.BookingItem;
import vn.edu.tmdt.booking.BookingItemRepository;
import vn.edu.tmdt.booking.BookingRepository;
import vn.edu.tmdt.catalog.Event;
import vn.edu.tmdt.catalog.EventRepository;
import vn.edu.tmdt.catalog.TicketType;
import vn.edu.tmdt.catalog.TicketTypeRepository;
import vn.edu.tmdt.catalog.Tour;
import vn.edu.tmdt.catalog.TourRepository;
import vn.edu.tmdt.catalog.TourSchedule;
import vn.edu.tmdt.catalog.TourScheduleRepository;
import vn.edu.tmdt.common.ApiResponse;
import vn.edu.tmdt.common.CurrentUser;
import vn.edu.tmdt.common.Slugs;
import vn.edu.tmdt.common.exception.AppException;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final EventRepository events;
    private final TicketTypeRepository tickets;
    private final TourRepository tours;
    private final TourScheduleRepository schedules;
    private final BookingRepository bookings;
    private final BookingItemRepository items;
    private final CurrentUser currentUser;
    private final IngestService ingest;
    private final LlmClient llm;

    public OrganizerController(EventRepository events, TicketTypeRepository tickets, TourRepository tours,
                               TourScheduleRepository schedules, BookingRepository bookings,
                               BookingItemRepository items, CurrentUser currentUser, IngestService ingest,
                               LlmClient llm) {
        this.events = events;
        this.tickets = tickets;
        this.tours = tours;
        this.schedules = schedules;
        this.bookings = bookings;
        this.items = items;
        this.currentUser = currentUser;
        this.ingest = ingest;
        this.llm = llm;
    }

    // ---------- SU KIEN ----------

    public record TicketInput(String name, BigDecimal price, Integer quota, Integer maxPerOrder) {
    }

    public record EventInput(String title, String category, String description, Long venueId,
                             String startAt, String endAt, String coverImageUrl, List<TicketInput> tickets) {
    }

    public record EventView(Long id, String title, String slug, String status, String category,
                            Instant startAt, String coverImageUrl, int sold, int quota) {
    }

    @GetMapping("/events")
    public ApiResponse<List<EventView>> myEvents(Authentication auth) {
        User user = currentUser.require(auth);
        return ApiResponse.ok(events.findByOrganizerIdOrderByStartAtDesc(user.getId()).stream()
                .map(event -> {
                    List<TicketType> types = tickets.findByEventId(event.id);
                    int sold = types.stream().mapToInt(t -> t.soldCount).sum();
                    int quota = types.stream().mapToInt(t -> t.quota).sum();
                    return new EventView(event.id, event.title, event.slug, event.status, event.category,
                            event.startAt, event.coverImageUrl, sold, quota);
                }).toList());
    }

    @PostMapping("/events")
    public ApiResponse<EventView> createEvent(@RequestBody EventInput input, Authentication auth) {
        User user = currentUser.require(auth);
        if (input.title() == null || input.title().isBlank()) {
            throw AppException.badRequest("Tieu de khong duoc de trong");
        }
        Event event = new Event();
        event.organizerId = user.getId();
        event.venueId = input.venueId();
        event.title = input.title().trim();
        event.slug = Slugs.of(event.title) + "-" + Long.toHexString(System.nanoTime()).substring(0, 4);
        event.category = input.category() == null ? "other" : input.category();
        event.description = input.description();
        event.coverImageUrl = input.coverImageUrl();
        event.startAt = parseInstant(input.startAt(), 20, 0);
        event.endAt = parseInstant(input.endAt(), 22, 30);
        event.status = "PENDING";
        events.save(event);

        int quota = 0;
        if (input.tickets() != null) {
            for (TicketInput t : input.tickets()) {
                if (t.name() == null || t.price() == null || t.quota() == null) {
                    continue;
                }
                TicketType type = new TicketType();
                type.eventId = event.id;
                type.name = t.name();
                type.price = t.price();
                type.quota = t.quota();
                type.maxPerOrder = t.maxPerOrder() == null ? 4 : t.maxPerOrder();
                tickets.save(type);
                quota += type.quota;
            }
        }
        ingest.indexEvent(event);
        return ApiResponse.ok("Da gui su kien cho quan tri duyet",
                new EventView(event.id, event.title, event.slug, event.status, event.category,
                        event.startAt, event.coverImageUrl, 0, quota));
    }

    // ---------- TOUR ----------

    public record ScheduleInput(String departureDate, String returnDate, BigDecimal price, Integer seatsTotal) {
    }

    public record TourInput(String title, String destination, String departureCity, Integer durationDays,
                            Integer durationNights, BigDecimal basePrice, String coverImageUrl,
                            String description, String highlights, String included, String excluded,
                            List<ScheduleInput> schedules) {
    }

    public record TourView(Long id, String title, String slug, String status, String destination,
                           Integer durationDays, BigDecimal basePrice, int seatsTotal, int seatsBooked) {
    }

    @GetMapping("/tours")
    public ApiResponse<List<TourView>> myTours(Authentication auth) {
        User user = currentUser.require(auth);
        return ApiResponse.ok(tours.findByOrganizerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(tour -> {
                    List<TourSchedule> list = schedules.findByTourIdOrderByDepartureDateAsc(tour.id);
                    return new TourView(tour.id, tour.title, tour.slug, tour.status, tour.destination,
                            tour.durationDays, tour.basePrice,
                            list.stream().mapToInt(s -> s.seatsTotal).sum(),
                            list.stream().mapToInt(s -> s.seatsBooked).sum());
                }).toList());
    }

    @PostMapping("/tours")
    public ApiResponse<TourView> createTour(@RequestBody TourInput input, Authentication auth) {
        User user = currentUser.require(auth);
        if (input.title() == null || input.title().isBlank()) {
            throw AppException.badRequest("Tieu de khong duoc de trong");
        }
        Tour tour = new Tour();
        tour.organizerId = user.getId();
        tour.title = input.title().trim();
        tour.slug = Slugs.of(tour.title) + "-" + Long.toHexString(System.nanoTime()).substring(0, 4);
        tour.destination = input.destination() == null ? "" : input.destination();
        tour.departureCity = input.departureCity() == null ? "" : input.departureCity();
        tour.durationDays = input.durationDays() == null ? 2 : input.durationDays();
        tour.durationNights = input.durationNights() == null ? 1 : input.durationNights();
        tour.basePrice = input.basePrice() == null ? BigDecimal.ZERO : input.basePrice();
        tour.coverImageUrl = input.coverImageUrl();
        tour.description = input.description();
        tour.highlights = input.highlights();
        tour.included = input.included();
        tour.excluded = input.excluded();
        tour.status = "PENDING";
        tours.save(tour);

        if (input.schedules() != null) {
            for (ScheduleInput s : input.schedules()) {
                if (s.departureDate() == null) {
                    continue;
                }
                TourSchedule schedule = new TourSchedule();
                schedule.tourId = tour.id;
                schedule.departureDate = LocalDate.parse(s.departureDate());
                schedule.returnDate = s.returnDate() == null
                        ? schedule.departureDate.plusDays(Math.max(1, tour.durationDays - 1))
                        : LocalDate.parse(s.returnDate());
                schedule.price = s.price() == null ? tour.basePrice : s.price();
                schedule.seatsTotal = s.seatsTotal() == null ? 20 : s.seatsTotal();
                schedule.seatsBooked = 0;
                schedule.status = "OPEN";
                schedules.save(schedule);
            }
        }
        ingest.indexTour(tour);
        return ApiResponse.ok("Da gui tour cho quan tri duyet",
                new TourView(tour.id, tour.title, tour.slug, tour.status, tour.destination,
                        tour.durationDays, tour.basePrice, 0, 0));
    }

    // ---------- DON HANG / THONG KE ----------

    public record BookingView(String code, String contactName, String contactEmail, String status,
                              BigDecimal totalAmount, Instant createdAt, String itemSummary) {
    }

    @GetMapping("/bookings")
    public ApiResponse<List<BookingView>> myBookings(Authentication auth) {
        User user = currentUser.require(auth);
        Set<Long> ticketIds = new HashSet<>();
        for (Event event : events.findByOrganizerIdOrderByStartAtDesc(user.getId())) {
            tickets.findByEventId(event.id).forEach(t -> ticketIds.add(t.id));
        }
        Set<Long> scheduleIds = new HashSet<>();
        for (Tour tour : tours.findByOrganizerIdOrderByCreatedAtDesc(user.getId())) {
            schedules.findByTourIdOrderByDepartureDateAsc(tour.id).forEach(s -> scheduleIds.add(s.id));
        }

        List<BookingView> result = new ArrayList<>();
        for (Booking booking : bookings.findAll()) {
            List<BookingItem> own = items.findByBookingId(booking.id).stream()
                    .filter(i -> ("TICKET".equals(i.itemType) && ticketIds.contains(i.refId))
                            || ("TOUR".equals(i.itemType) && scheduleIds.contains(i.refId)))
                    .toList();
            if (own.isEmpty()) {
                continue;
            }
            String summary = own.isEmpty() ? "" : own.get(0).itemName
                    + (own.size() > 1 ? " (+" + (own.size() - 1) + ")" : "");
            result.add(new BookingView(booking.code, booking.contactName, booking.contactEmail,
                    booking.status, booking.totalAmount, booking.createdAt, summary));
        }
        result.sort(Comparator.comparing(BookingView::createdAt).reversed());
        return ApiResponse.ok(result);
    }

    public record Stats(BigDecimal revenue, long orders, long ticketsSold, double fillRate,
                        long pendingEvents, long pendingTours) {
    }

    @GetMapping("/stats/overview")
    public ApiResponse<Stats> stats(Authentication auth) {
        User user = currentUser.require(auth);
        List<Event> myEvents = events.findByOrganizerIdOrderByStartAtDesc(user.getId());
        List<Tour> myTours = tours.findByOrganizerIdOrderByCreatedAtDesc(user.getId());

        Set<Long> ticketIds = new HashSet<>();
        int sold = 0;
        int quota = 0;
        for (Event event : myEvents) {
            for (TicketType type : tickets.findByEventId(event.id)) {
                ticketIds.add(type.id);
                sold += type.soldCount;
                quota += type.quota;
            }
        }
        Set<Long> scheduleIds = new HashSet<>();
        int seats = 0;
        int seatsTotal = 0;
        for (Tour tour : myTours) {
            for (TourSchedule schedule : schedules.findByTourIdOrderByDepartureDateAsc(tour.id)) {
                scheduleIds.add(schedule.id);
                seats += schedule.seatsBooked;
                seatsTotal += schedule.seatsTotal;
            }
        }

        BigDecimal revenue = BigDecimal.ZERO;
        Set<Long> orderIds = new HashSet<>();
        for (Booking booking : bookings.findAll()) {
            if (!"CONFIRMED".equals(booking.status)) {
                continue;
            }
            boolean mine = items.findByBookingId(booking.id).stream().anyMatch(i ->
                    ("TICKET".equals(i.itemType) && ticketIds.contains(i.refId))
                            || ("TOUR".equals(i.itemType) && scheduleIds.contains(i.refId)));
            if (mine) {
                orderIds.add(booking.id);
                revenue = revenue.add(booking.totalAmount);
            }
        }

        int totalQuota = quota + seatsTotal;
        int totalSold = sold + seats;
        double fillRate = totalQuota == 0 ? 0 : Math.round((totalSold * 1000.0 / totalQuota)) / 1000.0;
        long pendingEvents = myEvents.stream().filter(e -> "PENDING".equals(e.status)).count();
        long pendingTours = myTours.stream().filter(t -> "PENDING".equals(t.status)).count();

        return ApiResponse.ok(new Stats(revenue.setScale(2, RoundingMode.HALF_UP), orderIds.size(),
                totalSold, fillRate, pendingEvents, pendingTours));
    }

    public record CheckinRequest(String bookingCode, Long ticketId) {
    }

    @PostMapping("/checkin")
    public ApiResponse<String> checkin(@RequestBody CheckinRequest request, Authentication auth) {
        User user = currentUser.require(auth);
        Booking booking = bookings.findByCode(request.bookingCode())
                .orElseThrow(() -> AppException.notFound("Khong tim thay don hang"));
        BookingItem item = items.findByBookingId(booking.id).stream()
                .filter(i -> i.id.equals(request.ticketId()))
                .findFirst()
                .orElseThrow(() -> AppException.notFound("Ve khong thuoc don nay"));

        Set<Long> ticketIds = new HashSet<>();
        for (Event event : events.findByOrganizerIdOrderByStartAtDesc(user.getId())) {
            tickets.findByEventId(event.id).forEach(t -> ticketIds.add(t.id));
        }
        Set<Long> scheduleIds = new HashSet<>();
        for (Tour tour : tours.findByOrganizerIdOrderByCreatedAtDesc(user.getId())) {
            schedules.findByTourIdOrderByDepartureDateAsc(tour.id).forEach(s -> scheduleIds.add(s.id));
        }
        boolean mine = ("TICKET".equals(item.itemType) && ticketIds.contains(item.refId))
                || ("TOUR".equals(item.itemType) && scheduleIds.contains(item.refId));
        if (!mine) {
            throw AppException.forbidden("Ve nay khong thuoc su kien/tour cua ban");
        }
        if (item.checkedInAt != null) {
            throw AppException.conflict("Ve da duoc soat truoc do");
        }
        item.checkedInAt = Instant.now();
        items.save(item);
        return ApiResponse.ok("Soat ve thanh cong", "OK");
    }

    // ---------- AI ----------

    public record DescribeRequest(String destination, Integer days, String interests) {
    }

    public record DescribeResult(String description) {
    }

    @PostMapping("/ai/describe")
    public ApiResponse<DescribeResult> describe(@RequestBody DescribeRequest request) {
        String destination = request.destination() == null ? "Việt Nam" : request.destination();
        int days = request.days() == null ? 3 : request.days();
        String interests = request.interests() == null ? "khám phá, ẩm thực" : request.interests();

        String system = "Bạn là chuyên viên nội dung du lịch. Viết mô tả tour bằng tiếng Việt, "
                + "giọng thân thiện, 3 đoạn văn, mỗi đoạn 3-4 câu, không bịa giá cụ thể.";
        String prompt = "Viết mô tả cho tour " + days + " ngày tại " + destination
                + ", khách quan tâm: " + interests + ".";

        String generated = "";
        try {
            generated = llm.generate(system, prompt);
        } catch (RuntimeException ex) {
            generated = "";
        }
        if (generated == null || generated.isBlank()) {
            generated = "Tour " + days + " ngày tại " + destination + " dành cho khách yêu thích " + interests + ".\n\n"
                    + "Lịch trình được thiết kế cân bằng giữa tham quan và nghỉ ngơi, phù hợp cả gia đình và nhóm bạn. "
                    + "Đoàn di chuyển bằng xe đời mới, hướng dẫn viên giàu kinh nghiệm đồng hành suốt tuyến.\n\n"
                    + "Điểm nhấn của hành trình là những trải nghiệm địa phương, ẩm thực đặc trưng và thời gian tự do "
                    + "để khách khám phá theo cách riêng. Chi tiết giá và lịch khởi hành được cập nhật ở phần lịch trình.";
        }
        return ApiResponse.ok(new DescribeResult(generated.trim()));
    }

    private Instant parseInstant(String raw, int fallbackHour, int fallbackMinute) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now().plusDays(30).withHour(fallbackHour)
                    .withMinute(fallbackMinute).atZone(ZONE).toInstant();
        }
        try {
            return Instant.parse(raw);
        } catch (RuntimeException ignored) {
            return LocalDateTime.parse(raw).atZone(ZONE).toInstant();
        }
    }
}
