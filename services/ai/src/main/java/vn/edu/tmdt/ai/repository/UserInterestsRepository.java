package vn.edu.tmdt.ai.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import vn.edu.tmdt.ai.domain.UserInterests;

public interface UserInterestsRepository extends ReactiveMongoRepository<UserInterests, Long> {
}
