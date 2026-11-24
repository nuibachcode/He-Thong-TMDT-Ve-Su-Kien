package vn.edu.tmdt.ai.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import vn.edu.tmdt.ai.domain.TourCard;

public interface TourCardRepository extends ReactiveMongoRepository<TourCard, Long> {
}
