package vn.edu.tmdt.booking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByCodeAndUserId(String code, Long userId);

    Optional<Booking> findByCode(String code);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByStatusAndExpiresAtBefore(String status, Instant now);
}
