package vn.edu.tmdt.ai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.catalog.Tour;
import vn.edu.tmdt.catalog.TourRepository;

@Service
public class RecommendationService {

    public record Recommendation(Long id, String title, String slug, String destination,
                                 Integer durationDays, BigDecimal basePrice, String coverImageUrl,
                                 String reason) {
    }

    private final EmbeddingClient embedding;
    private final VectorStore vectorStore;
    private final TourRepository tours;

    public RecommendationService(EmbeddingClient embedding, VectorStore vectorStore, TourRepository tours) {
        this.embedding = embedding;
        this.vectorStore = vectorStore;
        this.tours = tours;
    }

    public List<Recommendation> recommend(User user, int limit) {
        String interests = user == null ? null : user.getInterests();

        if (interests != null && !interests.isBlank()) {
            float[] query = embedding.embed("Sở thích du lịch: " + interests);
            List<VectorStore.Hit> hits = vectorStore.search(query, 12, "TOUR", 0.01);
            Map<Long, Double> scores = new LinkedHashMap<>();
            for (VectorStore.Hit hit : hits) {
                if (hit.sourceId() != null) {
                    scores.merge(hit.sourceId(), hit.score(), Math::max);
                }
            }
            List<Recommendation> result = new ArrayList<>();
            for (Map.Entry<Long, Double> entry : scores.entrySet()) {
                tours.findById(entry.getKey())
                        .filter(tour -> "PUBLISHED".equals(tour.status))
                        .ifPresent(tour -> result.add(toRecommendation(tour,
                                "Phù hợp với sở thích: " + interests)));
                if (result.size() >= limit) {
                    break;
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }

        return tours.findByStatusOrderByCreatedAtDesc("PUBLISHED").stream()
                .limit(limit)
                .map(tour -> toRecommendation(tour, "Đang được nhiều khách quan tâm"))
                .toList();
    }

    private Recommendation toRecommendation(Tour tour, String reason) {
        return new Recommendation(tour.id, tour.title, tour.slug, tour.destination, tour.durationDays,
                tour.basePrice, tour.coverImageUrl, reason);
    }
}
