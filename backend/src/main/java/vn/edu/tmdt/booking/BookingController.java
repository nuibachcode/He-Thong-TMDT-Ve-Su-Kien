package vn.edu.tmdt.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.common.ApiResponse;
import vn.edu.tmdt.common.CurrentUser;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookings;
    private final CurrentUser currentUser;
    private final QrService qr;

    public BookingController(BookingService bookings, CurrentUser currentUser, QrService qr) {
        this.bookings = bookings;
        this.currentUser = currentUser;
        this.qr = qr;
    }

    public record CreateRequest(
            @NotBlank String bookingType,
            @NotEmpty @Valid List<ItemInput> items,
            String voucherCode,
            String contactName,
            @Email String contactEmail,
            String contactPhone,
            String note) {
    }

    public record ItemInput(Long refId, Integer quantity) {
    }

    public record ItemView(Long id, String itemName, Integer quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    public record Summary(String code, String bookingType, String status, BigDecimal totalAmount,
                          Instant createdAt, Instant expiresAt, int itemCount) {
    }

    public record Detail(String code, String bookingType, String status, BigDecimal subtotal,
                         BigDecimal discountAmount, BigDecimal totalAmount, String contactName,
                         String contactEmail, String contactPhone, String note, Instant createdAt,
                         Instant confirmedAt, Instant expiresAt, List<ItemView> items) {
    }

    public record Ticket(Long ticketId, String itemName, Integer quantity, String qrPngBase64) {
    }

    @PostMapping
    public ApiResponse<Summary> create(@Valid @RequestBody CreateRequest request, Authentication auth) {
        User user = currentUser.require(auth);
        var inputs = request.items().stream()
                .map(i -> new BookingService.ItemInput(i.refId(), i.quantity())).toList();
        Booking booking = bookings.create(user, request.bookingType(), inputs, request.voucherCode(),
                request.contactName(), request.contactEmail(), request.contactPhone(), request.note());
        return ApiResponse.ok("Da giu cho thanh cong", toSummary(booking));
    }

    @GetMapping
    public ApiResponse<List<Summary>> mine(Authentication auth) {
        User user = currentUser.require(auth);
        return ApiResponse.ok(bookings.listOf(user).stream().map(this::toSummary).toList());
    }

    @GetMapping("/{code}")
    public ApiResponse<Detail> detail(@PathVariable String code, Authentication auth) {
        User user = currentUser.require(auth);
        Booking booking = bookings.requireOwned(user, code);
        List<ItemView> items = bookings.itemsOf(booking.id).stream()
                .map(i -> new ItemView(i.id, i.itemName, i.quantity, i.unitPrice, i.amount)).toList();
        return ApiResponse.ok(new Detail(booking.code, booking.bookingType, booking.status, booking.subtotal,
                booking.discountAmount, booking.totalAmount, booking.contactName, booking.contactEmail,
                booking.contactPhone, booking.note, booking.createdAt, booking.confirmedAt,
                booking.expiresAt, items));
    }

    @PostMapping("/{code}/cancel")
    public ApiResponse<Summary> cancel(@PathVariable String code, Authentication auth) {
        User user = currentUser.require(auth);
        return ApiResponse.ok("Da huy don", toSummary(bookings.cancel(user, code)));
    }

    @GetMapping("/{code}/tickets")
    public ApiResponse<List<Ticket>> tickets(@PathVariable String code, Authentication auth) {
        User user = currentUser.require(auth);
        Booking booking = bookings.requireOwned(user, code);
        List<Ticket> result = bookings.itemsOf(booking.id).stream()
                .map(i -> {
                    String payload = "TICKET|" + booking.code + "|" + i.id + "|" + i.itemName;
                    return new Ticket(i.id, i.itemName, i.quantity, qr.pngBase64(payload, 260));
                })
                .toList();
        return ApiResponse.ok(result);
    }

    private Summary toSummary(Booking b) {
        return new Summary(b.code, b.bookingType, b.status, b.totalAmount, b.createdAt, b.expiresAt,
                bookings.itemsOf(b.id).size());
    }
}
