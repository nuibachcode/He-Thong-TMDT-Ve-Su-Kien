package vn.edu.tmdt.payment.service;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.payment.client.OrderClient;
import vn.edu.tmdt.payment.common.AppException;
import vn.edu.tmdt.payment.domain.Payment;
import vn.edu.tmdt.payment.gateway.PaymentGateway;
import vn.edu.tmdt.payment.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public record PaymentView(String status, String txnRef, java.math.BigDecimal amount, Instant paidAt) {
    }

    private final PaymentRepository payments;
    private final OrderClient orders;
    private final PaymentGateway gateway;

    public PaymentService(PaymentRepository payments, OrderClient orders, PaymentGateway gateway) {
        this.payments = payments;
        this.orders = orders;
        this.gateway = gateway;
    }

    @Transactional
    public PaymentView pay(String code, Long userId, String email) {
        if (userId == null) {
            throw AppException.unauthorized("Ban can dang nhap");
        }
        OrderClient.InternalBooking booking = orders.get(code);
        if (!userId.equals(booking.userId())) {
            throw AppException.unauthorized("Khong phai chu don hang");
        }
        Payment existing = payments.findByBookingCode(code).orElse(null);
        if (existing != null) {
            return view(existing);
        }
        if (!"PENDING".equals(booking.status())) {
            throw AppException.conflict("Don hang khong o trang thai cho thanh toan");
        }
        if (booking.expiresAt() != null && booking.expiresAt().isBefore(Instant.now())) {
            throw AppException.conflict("Don hang da het thoi gian giu cho");
        }

        Payment payment = new Payment();
        payment.bookingCode = code;
        payment.userId = userId;
        payment.amount = booking.totalAmount();
        payment.provider = gateway.provider();
        payment.txnRef = "PENDING-" + UUID.randomUUID();
        payment.status = "PENDING";
        payments.save(payment);

        PaymentGateway.Result result = gateway.pay(payment);
        if (!result.success()) {
            payment.status = "FAILED";
            payments.save(payment);
            throw AppException.conflict(result.message());
        }
        payment.txnRef = result.txnRef();
        payment.status = "SUCCESS";
        payment.paidAt = Instant.now();
        payments.save(payment);

        try {
            orders.confirm(code, userId, payment.txnRef, payment.provider);
        } catch (RuntimeException ex) {
            payment.status = "FAILED";
            payments.save(payment);
            log.warn("Thanh toan {} nhung xac nhan don that bai: {}", code, ex.getMessage());
            throw ex;
        }
        return view(payment);
    }

    @Transactional(readOnly = true)
    public PaymentView mockReturn(String txnRef) {
        Payment payment = payments.findByTxnRef(txnRef)
                .orElseThrow(() -> AppException.notFound("Khong tim thay giao dich"));
        return view(payment);
    }

    private PaymentView view(Payment payment) {
        return new PaymentView(payment.status, payment.txnRef, payment.amount, payment.paidAt);
    }
}
