package vn.edu.tmdt.ai.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SeedLoader {

    private final ObjectMapper mapper;
    private volatile Loaded loaded;

    public SeedLoader(ObjectMapper mapper) {
        this.mapper = mapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public Mono<Loaded> load() {
        if (loaded != null) {
            return Mono.just(loaded);
        }
        return Mono.fromCallable(() -> {
            Loaded value = new Loaded(
                    read("seed/events.json", new TypeReference<List<EventSeed>>() { }),
                    read("seed/tours.json", new TypeReference<List<TourSeed>>() { }),
                    read("seed/policies.json", new TypeReference<List<PolicySeed>>() { }));
            loaded = value;
            return value;
        });
    }

    private <T> List<T> read(String path, TypeReference<List<T>> type) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(input, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Khong doc duoc seed " + path, ex);
        }
    }

    public record Loaded(List<EventSeed> events, List<TourSeed> tours, List<PolicySeed> policies) {
    }

    public record EventSeed(Long id, String title, String slug, String category, String description,
                            String venue, String city, String startAt, long priceFrom,
                            String coverImageUrl, List<String> tags, int popularity) {
    }

    public record TourSeed(Long id, String title, String slug, String destination, String departureCity,
                           int durationDays, int durationNights, long basePrice, String coverImageUrl,
                           String description, List<String> highlights, List<String> included,
                           List<String> excluded, List<String> tags, int popularity) {
    }

    public record PolicySeed(Long id, String slug, String title, String content, List<String> tags) {
    }
}
