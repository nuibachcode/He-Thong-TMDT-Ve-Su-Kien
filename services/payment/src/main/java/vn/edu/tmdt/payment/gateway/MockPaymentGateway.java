package vn.edu.tmdt.payment.gateway;

import java.util.UUID;
import org.springframework.stereotype.Component;
import vn.edu.tmdt.payment.domain.Payment;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String provider() {
        return "MOCK";
    }

    @Override
    public Result pay(Payment payment) {
        String txnRef = "MOCK-" + payment.bookingCode + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return new Result(true, txnRef, "Thanh toan sandbox thanh cong");
    }
}
