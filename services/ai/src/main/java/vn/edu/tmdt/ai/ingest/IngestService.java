package vn.edu.tmdt.ai.ingest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.cache.AnswerCache;
import vn.edu.tmdt.ai.common.ApiException;
import vn.edu.tmdt.ai.domain.EventCard;
import vn.edu.tmdt.ai.domain.TourCard;
import vn.edu.tmdt.ai.provider.EmbeddingRouter;
import vn.edu.tmdt.ai.repository.EventCardRepository;
import vn.edu.tmdt.ai.repository.KnowledgeRepository;
import vn.edu.tmdt.ai.repository.TourCardRepository;

/**
 * Ingest: du lieu su kien/tour + tai lieu chinh sach -> chunk -> embedding -> pgvector.
 * Dong thoi dong bo card sang MongoDB cho recommendation.
 */
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    private static final NumberFormat MONEY = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));

    private final SeedLoader seedLoader;
    private final KnowledgeRepository knowledge;
    private final EmbeddingRouter embedding;
    private final EventCardRepository eventCards;
    private final TourCardRepository tourCards;
    private final AnswerCache answerCache;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public IngestService(SeedLoader seedLoader, KnowledgeRepository knowledge, EmbeddingRouter embedding,
                         EventCardRepository eventCards, TourCardRepository tourCards, AnswerCache answerCache) {
        this.seedLoader = seedLoader;
        this.knowledge = knowledge;
        this.embedding = embedding;
        this.eventCards = eventCards;
        this.tourCards = tourCards;
        this.answerCache = answerCache;
    }

    public Mono<Integer> reindexAll() {
        if (!running.compareAndSet(false, true)) {
            return Mono.error(ApiException.badRequest("Reindex dang chay, vui long doi"));
        }
        return seedLoader.load()
                .flatMap(seed -> saveCards(seed).then(buildRows(seed)))
                .flatMap(rows -> knowledge.deleteAll()
                        .thenMany(Flux.fromIterable(rows)
                                .concatMap(row -> knowledge.insert(row.sourceType(), row.sourceId(), row.title(),
                                        row.url(), row.content(), row.contentHash(), row.provider(), row.vector())))
                        .then(Mono.just(rows.size())))
                .flatMap(total -> answerCache.invalidate().thenReturn(total))
                .doOnSuccess(total -> log.info("Reindex xong: {} chunk (provider={})",
                        total, embedding.providerName()))
                .doFinally(signal -> running.set(false));
    }

    public Mono<Long> count() {
        return knowledge.count();
    }

    public Mono<Long> countForCurrentProvider() {
        return knowledge.countByProvider(embedding.providerName());
    }

    /** Dong bo card sang MongoDB ma khong can embed lai (dung khi khoi dong voi index da co). */
    public Mono<Void> syncCards() {
        return seedLoader.load().flatMap(this::saveCards);
    }

    public String provider() {
        return embedding.providerName();
    }

    private Mono<Void> saveCards(SeedLoader.Loaded seed) {
        List<EventCard> events = seed.events().stream().map(value -> {
            EventCard card = new EventCard();
            card.id = value.id();
            card.title = value.title();
            card.slug = value.slug();
            card.category = value.category();
            card.description = value.description();
            card.venue = value.venue();
            card.city = value.city();
            card.startAt = value.startAt();
            card.priceFrom = value.priceFrom();
            card.coverImageUrl = value.coverImageUrl();
            card.tags = value.tags() == null ? List.of() : value.tags();
            card.popularity = value.popularity();
            return card;
        }).toList();

        List<TourCard> tours = seed.tours().stream().map(value -> {
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
            card.tags = value.tags() == null ? List.of() : value.tags();
            card.popularity = value.popularity();
            return card;
        }).toList();

        return eventCards.saveAll(events)
                .thenMany(tourCards.saveAll(tours))
                .then()
                .onErrorResume(error -> {
                    log.warn("Khong dong bo duoc card sang MongoDB: {}", error.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<List<ChunkRow>> buildRows(SeedLoader.Loaded seed) {
        List<SourceDoc> docs = new ArrayList<>();
        for (SeedLoader.EventSeed event : seed.events()) {
            docs.add(new SourceDoc("EVENT", event.id(), event.title(), "/events/" + event.slug(),
                    eventText(event)));
        }
        for (SeedLoader.TourSeed tour : seed.tours()) {
            docs.add(new SourceDoc("TOUR", tour.id(), tour.title(), "/tours/" + tour.slug(),
                    tourText(tour)));
        }
        for (SeedLoader.PolicySeed policy : seed.policies()) {
            docs.add(new SourceDoc("POLICY", policy.id(), policy.title(), null, policyText(policy)));
        }

        String provider = embedding.providerName();
        return Flux.fromIterable(docs)
                .concatMap(doc -> {
                    List<Chunker.Chunk> chunks = Chunker.split(doc.text());
                    return Flux.range(0, chunks.size())
                            .concatMap(index -> {
                                Chunker.Chunk chunk = chunks.get(index);
                                String content = doc.title() + "\n" + chunk.content();
                                String hash = sha256(provider + "|" + doc.type() + "|" + doc.id()
                                        + "|" + index + "|" + content);
                                return embedding.embed(content)
                                        .map(vector -> new ChunkRow(doc.type(), doc.id(), doc.title(),
                                                doc.url(), content, hash, provider, fit(vector)));
                            });
                })
                .collectList()
                .flatMap(rows -> {
                    if (rows.isEmpty()) {
                        return Mono.error(new IllegalStateException("Khong tao duoc chunk nao"));
                    }
                    return Mono.just(rows);
                });
    }

    private float[] fit(float[] vector) {
        int dimension = embedding.active().dimension();
        if (vector.length == dimension) {
            return vector;
        }
        float[] fitted = new float[dimension];
        System.arraycopy(vector, 0, fitted, 0, Math.min(vector.length, dimension));
        return fitted;
    }

    private String eventText(SeedLoader.EventSeed event) {
        return """
                Sự kiện: %s
                Danh mục: %s
                Thời gian: %s
                Địa điểm: %s, %s
                Giá vé từ: %s VND
                Mô tả: %s
                Từ khóa: %s
                """.formatted(event.title(), event.category(), event.startAt(), event.venue(), event.city(),
                MONEY.format(event.priceFrom()), event.description(), join(event.tags()));
    }

    private String tourText(SeedLoader.TourSeed tour) {
        return """
                Tour: %s
                Điểm đến: %s
                Khởi hành từ: %s
                Thời lượng: %d ngày %d đêm
                Giá cơ bản: %s VND
                Mô tả: %s
                Điểm nhấn: %s
                Bao gồm: %s
                Không bao gồm: %s
                Từ khóa: %s
                """.formatted(tour.title(), tour.destination(), tour.departureCity(),
                tour.durationDays(), tour.durationNights(), MONEY.format(tour.basePrice()),
                tour.description(), join(tour.highlights()), join(tour.included()),
                join(tour.excluded()), join(tour.tags()));
    }

    private String policyText(SeedLoader.PolicySeed policy) {
        return """
                Chính sách: %s
                %s
                Từ khóa: %s
                """.formatted(policy.title(), policy.content(), join(policy.tags()));
    }

    private static String join(List<String> values) {
        return values == null ? "" : String.join(", ", values);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private record SourceDoc(String type, Long id, String title, String url, String text) {
    }

    private record ChunkRow(String sourceType, Long sourceId, String title, String url, String content,
                            String contentHash, String provider, float[] vector) {
    }
}
