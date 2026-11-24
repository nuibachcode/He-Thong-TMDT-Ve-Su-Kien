package vn.edu.tmdt.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findByEventId(Long eventId);

    @Modifying
    @Query("update TicketType t set t.soldCount = t.soldCount + :qty "
            + "where t.id = :id and t.soldCount + :qty <= t.quota")
    int reserve(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("update TicketType t set t.soldCount = t.soldCount - :qty "
            + "where t.id = :id and t.soldCount >= :qty")
    int release(@Param("id") Long id, @Param("qty") int qty);
}
