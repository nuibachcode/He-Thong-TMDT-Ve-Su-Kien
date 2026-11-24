package vn.edu.tmdt.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourRepository extends JpaRepository<Tour, Long> {

    Optional<Tour> findBySlug(String slug);

    List<Tour> findByStatusOrderByCreatedAtDesc(String status);

    List<Tour> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    @Query("""
            select t from Tour t
            where t.status = 'PUBLISHED'
              and (:q is null or lower(t.title) like lower(concat('%', :q, '%')))
              and (:destination is null or lower(t.destination) like lower(concat('%', :destination, '%')))
            order by t.createdAt desc
            """)
    List<Tour> search(@Param("q") String q, @Param("destination") String destination);
}
