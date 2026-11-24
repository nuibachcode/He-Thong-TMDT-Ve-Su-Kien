package vn.edu.tmdt.order.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.order.common.ApiResponse;
import vn.edu.tmdt.order.service.BookingService;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerOrderController {

    private final BookingService bookings;

    public OrganizerOrderController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/bookings")
    public ApiResponse<List<BookingService.OrgBooking>> bookings(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(bookings.organizerBookings(userId));
    }

    @GetMapping("/stats/overview")
    public ApiResponse<BookingService.OrgStats> stats(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(bookings.organizerStats(userId));
    }
}
