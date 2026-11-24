package vn.edu.tmdt.order.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.order.service.BookingService;

@RestController
@RequestMapping("/internal/bookings")
public class InternalBookingController {

    private final BookingService bookings;

    public InternalBookingController(BookingService bookings) {
        this.bookings = bookings;
    }

    @GetMapping("/{code}")
    public BookingService.InternalBooking get(@PathVariable String code) {
        return bookings.internalGet(code);
    }

    @PostMapping("/{code}/confirm")
    public BookingService.InternalBooking confirm(@PathVariable String code,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String txnRef,
                                                  @RequestParam(required = false) String provider) {
        return bookings.internalConfirm(code, userId, txnRef, provider);
    }
}
