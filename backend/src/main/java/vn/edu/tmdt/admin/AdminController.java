package vn.edu.tmdt.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.ai.AiFeedbackRepository;
import vn.edu.tmdt.ai.ChatMessageRepository;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.auth.UserRepository;
import vn.edu.tmdt.booking.Booking;
import vn.edu.tmdt.booking.BookingRepository;
import vn.edu.tmdt.catalog.Event;
import vn.edu.tmdt.catalog.EventRepository;
import vn.edu.tmdt.catalog.Tour;
import vn.edu.tmdt.catalog.TourRepository;
import vn.edu.tmdt.common.ApiResponse;
import vn.edu.tmdt.common.exception.AppException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;
    private final EventRepository events;
    private final TourRepository tours;
    private final BookingRepository bookings;
    private final ChatMessageRepository messages;
    private final AiFeedbackRepository feedback;

    public AdminController(UserRepository users, EventRepository events, TourRepository tours,
                           BookingRepository bookings, ChatMessageRepository messages,
                           AiFeedbackRepository feedback) {
        this.users = users;
        this.events = events;
        this.tours = tours;
        this.bookings = bookings;
        this.messages = messages;
        this.feedback = feedback;
    }

    public record UserView(Long id, String email, String fullName, String role, boolean enabled,
                           Instant createdAt) {
    }

    @GetMapping("/users")
    public ApiResponse<List<UserView>> listUsers(@RequestParam(required = false) String q) {
        String needle = q == null ? null : q.trim().toLowerCase();
        return ApiResponse.ok(users.findAll().stream()
                .filter(u -> needle == null || needle.isBlank()
                        || u.getEmail().toLowerCase().contains(needle)
                        || u.getFullName().toLowerCase().contains(needle))
                .map(u -> new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(),
                        u.isEnabled(), u.getCreatedAt()))
                .toList());
    }

    public record StatusRequest(boolean enabled) {
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<String> setUserStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        User user = users.findById(id).orElseThrow(() -> AppException.notFound("Khong tim thay nguoi dung"));
        user.setEnabled(request.enabled());
        users.save(user);
        return ApiResponse.ok(request.enabled() ? "Da mo khoa tai khoan" : "Da khoa tai khoan", "OK");
    }

    public record PendingView(Long id, String title, String type, String status, Instant createdAt) {
    }

    @GetMapping("/pending")
    public ApiResponse<List<PendingView>> pending() {
        List<PendingView> result = new java.util.ArrayList<>();
        events.findByStatusOrderByStartAtAsc("PENDING")
                .forEach(e -> result.add(new PendingView(e.id, e.title, "EVENT", e.status, e.createdAt)));
        tours.findByStatusOrderByCreatedAtDesc("PENDING")
                .forEach(t -> result.add(new PendingView(t.id, t.title, "TOUR", t.status, t.createdAt)));
        return ApiResponse.ok(result);
    }

    public record ContentStatusRequest(String status) {
    }

    @PatchMapping("/content/{type}/{id}/status")
    public ApiResponse<String> approveContent(@PathVariable String type, @PathVariable Long id,
                                              @RequestBody ContentStatusRequest request) {
        String status = request.status() == null ? "" : request.status().toUpperCase();
        if (!List.of("PUBLISHED", "REJECTED", "PENDING").contains(status)) {
            throw AppException.badRequest("Trang thai khong hop le");
        }
        if ("EVENT".equalsIgnoreCase(type)) {
            Event event = events.findById(id)
                    .orElseThrow(() -> AppException.notFound("Khong tim thay su kien"));
            event.status = status;
            events.save(event);
        } else if ("TOUR".equalsIgnoreCase(type)) {
            Tour tour = tours.findById(id).orElseThrow(() -> AppException.notFound("Khong tim thay tour"));
            tour.status = status;
            tours.save(tour);
        } else {
            throw AppException.badRequest("Loai noi dung khong hop le");
        }
        return ApiResponse.ok("Da cap nhat trang thai: " + status, "OK");
    }

    public record Stats(long users, long events, long tours, long bookings, BigDecimal revenue) {
    }

    @GetMapping("/stats/overview")
    public ApiResponse<Stats> stats() {
        BigDecimal revenue = bookings.findAll().stream()
                .filter(b -> "CONFIRMED".equals(b.status))
                .map(b -> b.totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ApiResponse.ok(new Stats(users.count(), events.count(), tours.count(), bookings.count(), revenue));
    }

    public record AiMetrics(long questions, long answers, long fallback, long refused,
                            double avgLatencyMs, double feedbackAvg, long positive, long negative) {
    }

    @GetMapping("/ai/metrics")
    public ApiResponse<AiMetrics> aiMetrics() {
        long answers = messages.countByRole("ASSISTANT");
        return ApiResponse.ok(new AiMetrics(
                messages.countByRole("USER"),
                answers,
                messages.countByFallbackTrue(),
                messages.countByRefusedTrue(),
                Math.round(messages.averageLatency() * 10) / 10.0,
                Math.round(feedback.averageRating() * 100) / 100.0,
                feedback.countByRating(1),
                feedback.countByRating(-1)));
    }
}
