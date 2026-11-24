package vn.edu.tmdt.inventory.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tmdt.inventory.domain.Hold;

public interface HoldRepository extends JpaRepository<Hold, Long> {

    List<Hold> findByBookingCodeOrderByIdAsc(String bookingCode);

    List<Hold> findByBookingCodeAndStatus(String bookingCode, String status);

    List<Hold> findByStatusAndExpiresAtBefore(String status, Instant now);
}
