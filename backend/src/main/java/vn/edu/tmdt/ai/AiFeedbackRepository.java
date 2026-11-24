package vn.edu.tmdt.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    long countByRating(Integer rating);

    @Query("select coalesce(avg(f.rating), 0) from AiFeedback f")
    double averageRating();
}
