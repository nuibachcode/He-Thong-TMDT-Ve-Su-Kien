package vn.edu.tmdt.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tmdt.order.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByCode(String code);

    Optional<Booking> findByCodeAndUserId(String code, Long userId);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByStatusAndExpiresAtBefore(String status, Instant now);

    List<Booking> findByStatus(String status);
}
