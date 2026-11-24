package vn.edu.tmdt.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.tmdt.inventory.domain.Stock;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByRefTypeAndRefId(String refType, Long refId);

    List<Stock> findByRefTypeAndRefIdIn(String refType, Collection<Long> refIds);

    List<Stock> findByOrganizerId(Long organizerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Stock s set s.sold = s.sold + :qty "
            + "where s.refType = :refType and s.refId = :refId and s.sold + :qty <= s.total")
    int reserve(@Param("refType") String refType, @Param("refId") Long refId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Stock s set s.sold = s.sold - :qty "
            + "where s.refType = :refType and s.refId = :refId and s.sold >= :qty")
    int release(@Param("refType") String refType, @Param("refId") Long refId, @Param("qty") int qty);
}
