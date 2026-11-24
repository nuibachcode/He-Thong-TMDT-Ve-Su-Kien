package vn.edu.tmdt.catalog.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import vn.edu.tmdt.catalog.domain.Tour;

public interface TourRepository extends MongoRepository<Tour, Long> {

    List<Tour> findByStatusOrderByCreatedAtDesc(String status);

    List<Tour> findByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    Optional<Tour> findBySlug(String slug);

    long countByStatus(String status);
}
