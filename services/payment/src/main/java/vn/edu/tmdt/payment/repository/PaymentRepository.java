package vn.edu.tmdt.payment.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tmdt.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingCode(String bookingCode);

    Optional<Payment> findByTxnRef(String txnRef);
}
