package vn.edu.tmdt.order.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.order.client.InventoryClient;
import vn.edu.tmdt.order.common.ApiResponse;
import vn.edu.tmdt.order.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    public record CreateRequest(@NotBlank String bookingType,
                                @NotEmpty @Valid List<BookingService.ItemInput> items,
                                String voucherCode,
                                String contactName,
                                @Email String contactEmail,
                                String contactPhone,
                                String note) {
    }

    private final BookingService bookings;

    public BookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @PostMapping
    public ApiResponse<BookingService.Summary> create(@Valid @RequestBody CreateRequest request,
                                                      @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                                      @RequestHeader(value = "X-User-Email", required = false) String email) {
        return ApiResponse.ok("Da giu cho thanh cong", bookings.create(userId, email, request.bookingType(),
                request.items(), request.voucherCode(), request.contactName(), request.contactEmail(),
                request.contactPhone(), request.note()));
    }

    @GetMapping
    public ApiResponse<List<BookingService.Summary>> mine(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(bookings.listOf(userId));
    }

    @GetMapping("/{code}")
    public ApiResponse<BookingService.Detail> detail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable String code) {
        return ApiResponse.ok(bookings.detail(userId, code));
    }

    @PostMapping("/{code}/cancel")
    public ApiResponse<BookingService.Summary> cancel(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable String code) {
        return ApiResponse.ok("Da huy don", bookings.cancel(userId, code));
    }

    @GetMapping("/{code}/tickets")
    public ApiResponse<List<InventoryClient.TicketView>> tickets(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable String code) {
        return ApiResponse.ok(bookings.tickets(userId, code));
    }
}
