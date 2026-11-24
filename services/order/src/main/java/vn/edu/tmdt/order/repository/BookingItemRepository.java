package vn.edu.tmdt.order.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tmdt.order.domain.BookingItem;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {

    List<BookingItem> findByBookingId(Long bookingId);

    List<BookingItem> findByBookingIdIn(List<Long> bookingIds);

    List<BookingItem> findByOrganizerId(Long organizerId);
}
