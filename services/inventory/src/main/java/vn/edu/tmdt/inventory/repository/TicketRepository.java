package vn.edu.tmdt.inventory.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tmdt.inventory.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByBookingCodeOrderByIdAsc(String bookingCode);

    Optional<Ticket> findByIdAndBookingCode(Long id, String bookingCode);

    void deleteByBookingCode(String bookingCode);
}
