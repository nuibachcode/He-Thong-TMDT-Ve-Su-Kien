package vn.edu.tmdt.order.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.order.common.ApiResponse;
import vn.edu.tmdt.order.service.BookingService;

@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private final BookingService bookings;

    public AdminStatsController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/stats/overview")
    public ApiResponse<BookingService.AdminStats> stats() {
        return ApiResponse.ok(bookings.adminStats());
    }
}
