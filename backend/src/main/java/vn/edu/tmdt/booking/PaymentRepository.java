package vn.edu.tmdt.booking;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findFirstByBookingIdOrderByPaidAtDesc(Long bookingId);
}
