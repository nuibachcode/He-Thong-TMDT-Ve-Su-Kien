package vn.edu.tmdt.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import vn.edu.tmdt.catalog.domain.Event;

public interface EventRepository extends MongoRepository<Event, Long> {

    List<Event> findByStatusOrderByStartAtAsc(String status);

    List<Event> findByStatusOrderByCreatedAtDesc(String status);

    List<Event> findByOrganizerIdOrderByStartAtDesc(Long organizerId);

    Optional<Event> findBySlug(String slug);

    long countByStatus(String status);
}
