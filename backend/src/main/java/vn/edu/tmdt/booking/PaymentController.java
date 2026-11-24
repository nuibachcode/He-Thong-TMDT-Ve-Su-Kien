package vn.edu.tmdt.booking;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.common.ApiResponse;
import vn.edu.tmdt.common.CurrentUser;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final BookingService bookings;
    private final CurrentUser currentUser;

    public PaymentController(BookingService bookings, CurrentUser currentUser) {
        this.bookings = bookings;
        this.currentUser = currentUser;
    }

    public record PaymentView(String status, String txnRef, BigDecimal amount, Instant paidAt) {
    }

    /** Cong thanh toan mo phong: xac nhan don ngay lap tuc. */
    @PostMapping("/{code}/pay")
    public ApiResponse<PaymentView> pay(@PathVariable String code, Authentication auth) {
        User user = currentUser.require(auth);
        Payment payment = bookings.pay(user, code);
        return ApiResponse.ok("Thanh toan thanh cong",
                new PaymentView(payment.status, payment.txnRef, payment.amount, payment.paidAt));
    }
}
