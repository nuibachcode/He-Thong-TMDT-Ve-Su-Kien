package vn.edu.tmdt.ai.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.config.AiProperties;
import vn.edu.tmdt.ai.domain.Citation;

/**
 * Cache cau tra loi pho bien trong Redis de tiet kiem quota Gemini.
 * Key gan voi version index: reindex xong se tang version va cache cu tu het hieu luc.
 */
@Component
public class AnswerCache {

    private static final Logger log = LoggerFactory.getLogger(AnswerCache.class);
    private static final String VERSION_KEY = "ai:cache:version";

    private final ReactiveStringRedisTemplate redis;
    private final AiProperties properties;
    private final ObjectMapper mapper;

    public AnswerCache(ReactiveStringRedisTemplate redis, AiProperties properties, ObjectMapper mapper) {
        this.redis = redis;
        this.properties = properties;
        this.mapper = mapper;
    }

    public Mono<CachedAnswer> get(String question, String provider) {
        if (!properties.cache().enabled()) {
            return Mono.empty();
        }
        return version()
                .flatMap(version -> redis.opsForValue().get(key(question, provider, version)))
                .flatMap(json -> Mono.fromCallable(() -> mapper.readValue(json, CachedAnswer.class)))
                .onErrorResume(error -> Mono.empty());
    }

    public Mono<Void> put(String question, String provider, CachedAnswer answer) {
        if (!properties.cache().enabled()) {
            return Mono.empty();
        }
        return version()
                .flatMap(version -> Mono.fromCallable(() -> mapper.writeValueAsString(answer))
                        .flatMap(json -> redis.opsForValue()
                                .set(key(question, provider, version), json,
                                        Duration.ofSeconds(properties.cache().ttlSeconds()))))
                .onErrorResume(error -> {
                    log.debug("Khong ghi duoc answer cache: {}", error.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    public Mono<Void> invalidate() {
        return redis.opsForValue().increment(VERSION_KEY)
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    private Mono<Long> version() {
        return redis.opsForValue().get(VERSION_KEY)
                .map(Long::parseLong)
                .defaultIfEmpty(1L)
                .onErrorResume(error -> Mono.just(1L));
    }

    private static String key(String question, String provider, long version) {
        return "ai:ans:" + version + ":" + provider + ":" + sha256(question.toLowerCase());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    public record CachedAnswer(String answer, List<Citation> citations, boolean refused, boolean fallback) {
    }
}
