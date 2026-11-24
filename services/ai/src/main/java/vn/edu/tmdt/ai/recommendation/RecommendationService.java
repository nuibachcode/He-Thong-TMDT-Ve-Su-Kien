package vn.edu.tmdt.ai.recommendation;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.domain.EventCard;
import vn.edu.tmdt.ai.domain.Recommendation;
import vn.edu.tmdt.ai.domain.TourCard;
import vn.edu.tmdt.ai.domain.UserInterests;
import vn.edu.tmdt.ai.ingest.SeedLoader;
import vn.edu.tmdt.ai.repository.EventCardRepository;
import vn.edu.tmdt.ai.repository.TourCardRepository;
import vn.edu.tmdt.ai.repository.UserInterestsRepository;

@Service
public class RecommendationService {

    private final EventCardRepository eventCards;
    private final TourCardRepository tourCards;
    private final UserInterestsRepository interestsRepository;
    private final SeedLoader seedLoader;

    public RecommendationService(EventCardRepository eventCards, TourCardRepository tourCards,
                                 UserInterestsRepository interestsRepository, SeedLoader seedLoader) {
        this.eventCards = eventCards;
        this.tourCards = tourCards;
        this.interestsRepository = interestsRepository;
        this.seedLoader = seedLoader;
    }

    public Mono<List<Recommendation>> recommend(Long userId, List<String> requestedInterests, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));
        Mono<List<String>> effectiveInterests = requestedInterests == null || requestedInterests.isEmpty()
                ? savedInterests(userId)
                : Mono.just(normalizeInterests(requestedInterests));

        return Mono.zip(effectiveInterests, loadEvents(), loadTours())
                .map(tuple -> rank(tuple.getT1(), tuple.getT2(), tuple.getT3(), safeLimit));
    }

    public Mono<List<String>> savedInterests(Long userId) {
        if (userId == null) {
            return Mono.just(List.of());
        }
        return interestsRepository.findById(userId)
                .map(value -> value.interests == null ? List.<String>of() : value.interests)
                .defaultIfEmpty(List.of())
                .onErrorResume(error -> Mono.just(List.of()));
    }

    public Mono<List<String>> saveInterests(Long userId, List<String> input) {
        UserInterests document = new UserInterests();
        document.userId = userId;
        document.interests = normalizeInterests(input);
        document.updatedAt = Instant.now();
        return interestsRepository.save(document).map(saved -> saved.interests);
    }

    private Mono<List<EventCard>> loadEvents() {
        return eventCards.findAll().collectList()
                .flatMap(list -> list.isEmpty() ? fallbackEvents() : Mono.just(list))
                .onErrorResume(error -> fallbackEvents());
    }

    private Mono<List<TourCard>> loadTours() {
        return tourCards.findAll().collectList()
                .flatMap(list -> list.isEmpty() ? fallbackTours() : Mono.just(list))
                .onErrorResume(error -> fallbackTours());
    }

    private Mono<List<EventCard>> fallbackEvents() {
        return seedLoader.load().map(seed -> seed.events().stream().map(value -> {
            EventCard card = new EventCard();
            card.id = value.id();
            card.title = value.title();
            card.slug = value.slug();
            card.category = value.category();
            card.venue = value.venue();
            card.city = value.city();
            card.startAt = value.startAt();
            card.priceFrom = value.priceFrom();
            card.coverImageUrl = value.coverImageUrl();
            card.tags = value.tags();
            card.popularity = value.popularity();
            return card;
        }).toList());
    }

    private Mono<List<TourCard>> fallbackTours() {
        return seedLoader.load().map(seed -> seed.tours().stream().map(value -> {
            TourCard card = new TourCard();
            card.id = value.id();
            card.title = value.title();
            card.slug = value.slug();
            card.destination = value.destination();
            card.departureCity = value.departureCity();
            card.durationDays = value.durationDays();
            card.durationNights = value.durationNights();
            card.basePrice = value.basePrice();
            card.coverImageUrl = value.coverImageUrl();
            card.tags = value.tags();
            card.popularity = value.popularity();
            return card;
        }).toList());
    }

    private List<Recommendation> rank(List<String> interests, List<EventCard> events,
                                      List<TourCard> tours, int limit) {
        List<Scored> scored = new ArrayList<>();
        for (EventCard event : events) {
            int overlap = overlap(interests, event.tags);
            scored.add(new Scored(new Recommendation("EVENT", event.id, event.title, event.slug,
                    event.venue + " · " + event.city, event.priceFrom, event.coverImageUrl,
                    reason(interests, overlap, event.tags), "/events/" + event.slug, tags(event.tags)),
                    overlap * 1000 + event.popularity));
        }
        for (TourCard tour : tours) {
            int overlap = overlap(interests, tour.tags);
            String subtitle = tour.destination + " · " + tour.durationDays + "N" + tour.durationNights + "Đ";
            scored.add(new Scored(new Recommendation("TOUR", tour.id, tour.title, tour.slug,
                    subtitle, tour.basePrice, tour.coverImageUrl,
                    reason(interests, overlap, tour.tags), "/tours/" + tour.slug, tags(tour.tags)),
                    overlap * 1000 + tour.popularity));
        }
        return scored.stream()
                .sorted(Comparator.comparingInt(Scored::score).reversed())
                .limit(limit)
                .map(Scored::recommendation)
                .toList();
    }

    private String reason(List<String> interests, int overlap, List<String> tags) {
        if (interests.isEmpty()) {
            return "Đang được nhiều người quan tâm";
        }
        if (overlap > 0) {
            return "Phù hợp sở thích: " + String.join(", ", matched(interests, tags));
        }
        return "Gợi ý thêm để bạn khám phá";
    }

    private int overlap(List<String> interests, List<String> tags) {
        if (interests.isEmpty() || tags == null) {
            return 0;
        }
        List<String> normalizedTags = tags.stream().map(this::fold).toList();
        int count = 0;
        for (String interest : interests) {
            String folded = fold(interest);
            if (normalizedTags.stream().anyMatch(tag -> tag.contains(folded) || folded.contains(tag))) {
                count++;
            }
        }
        return count;
    }

    private List<String> matched(List<String> interests, List<String> tags) {
        List<String> result = new ArrayList<>();
        if (tags == null) {
            return result;
        }
        List<String> normalizedTags = tags.stream().map(this::fold).toList();
        for (String interest : interests) {
            String folded = fold(interest);
            if (normalizedTags.stream().anyMatch(tag -> tag.contains(folded) || folded.contains(tag))) {
                result.add(interest);
            }
        }
        return result.isEmpty() ? interests : result;
    }

    private List<String> tags(List<String> tags) {
        return tags == null ? List.of() : tags.stream().limit(4).toList();
    }

    private List<String> normalizeInterests(List<String> input) {
        if (input == null) {
            return List.of();
        }
        return input.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(value -> value.length() > 40 ? value.substring(0, 40) : value)
                .distinct()
                .limit(10)
                .toList();
    }

    private String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private record Scored(Recommendation recommendation, int score) {
    }
}
