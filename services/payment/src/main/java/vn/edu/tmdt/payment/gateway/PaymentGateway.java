package vn.edu.tmdt.payment.gateway;

import vn.edu.tmdt.payment.domain.Payment;

/**
 * Cong thanh toan sandbox noi bo. Co the thay bang adapter VNPay/MoMo
 * (cung interface) khi co merchant code va chu ky HMAC.
 */
public interface PaymentGateway {

    String provider();

    Result pay(Payment payment);

    record Result(boolean success, String txnRef, String message) {
    }
}
