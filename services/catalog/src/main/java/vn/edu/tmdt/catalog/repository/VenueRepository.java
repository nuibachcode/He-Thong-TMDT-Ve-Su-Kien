package vn.edu.tmdt.catalog.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import vn.edu.tmdt.catalog.domain.Venue;

public interface VenueRepository extends MongoRepository<Venue, Long> {
}
