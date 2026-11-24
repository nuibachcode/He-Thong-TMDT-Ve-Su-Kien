package vn.edu.tmdt.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    java.util.Optional<Event> findBySlug(String slug);

    List<Event> findByStatusOrderByStartAtAsc(String status);

    List<Event> findByOrganizerIdOrderByStartAtDesc(Long organizerId);

    @Query("""
            select e from Event e
            where e.status = 'PUBLISHED'
              and (:q is null or lower(e.title) like lower(concat('%', :q, '%')))
              and (:category is null or e.category = :category)
            order by e.startAt asc
            """)
    List<Event> search(@Param("q") String q, @Param("category") String category);
}
