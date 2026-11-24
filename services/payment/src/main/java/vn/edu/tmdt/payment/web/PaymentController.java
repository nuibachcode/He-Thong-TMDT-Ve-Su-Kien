package vn.edu.tmdt.payment.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.payment.common.ApiResponse;
import vn.edu.tmdt.payment.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/{code}/pay")
    public ApiResponse<PaymentService.PaymentView> pay(
            @PathVariable String code,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return ApiResponse.ok("Thanh toan thanh cong", payments.pay(code, userId, email));
    }

    @GetMapping("/mock-return")
    public ApiResponse<PaymentService.PaymentView> mockReturn(@RequestParam String txnRef) {
        return ApiResponse.ok(payments.mockReturn(txnRef));
    }
}
