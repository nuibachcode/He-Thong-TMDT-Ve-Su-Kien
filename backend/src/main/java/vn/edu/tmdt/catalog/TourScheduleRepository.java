package vn.edu.tmdt.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourScheduleRepository extends JpaRepository<TourSchedule, Long> {

    List<TourSchedule> findByTourIdOrderByDepartureDateAsc(Long tourId);

    @Modifying
    @Query("update TourSchedule s set s.seatsBooked = s.seatsBooked + :qty "
            + "where s.id = :id and s.seatsBooked + :qty <= s.seatsTotal")
    int reserve(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update TourSchedule s set s.seatsBooked = s.seatsBooked - :qty "
            + "where s.id = :id and s.seatsBooked >= :qty")
    int release(@Param("id") Long id, @Param("qty") int qty);
}
