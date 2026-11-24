package vn.edu.tmdt.ai.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import vn.edu.tmdt.ai.domain.EventCard;

public interface EventCardRepository extends ReactiveMongoRepository<EventCard, Long> {
}
