package vn.edu.tmdt.ai.provider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.config.AiProperties;

/**
 * Chon provider embedding (Gemini khi co key, nguoc lai lexical) va cache vector trong Redis
 * de tiet kiem quota khi reindex nhieu lan.
 */
@Component
public class EmbeddingRouter {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingRouter.class);

    private final AiProperties properties;
    private final GeminiEmbeddingProvider gemini;
    private final LexicalEmbeddingProvider lexical;
    private final ReactiveStringRedisTemplate redis;

    public EmbeddingRouter(AiProperties properties, GeminiEmbeddingProvider gemini,
                           LexicalEmbeddingProvider lexical, ReactiveStringRedisTemplate redis) {
        this.properties = properties;
        this.gemini = gemini;
        this.lexical = lexical;
        this.redis = redis;
    }

    public EmbeddingProvider active() {
        return properties.geminiEnabled() ? gemini : lexical;
    }

    public String providerName() {
        return active().name();
    }

    public Mono<float[]> embed(String text) {
        EmbeddingProvider provider = active();
        if (!properties.cache().enabled()) {
            return provider.embed(text);
        }
        String cacheKey = "ai:embed:" + provider.name() + ":" + sha256(text);
        return redis.opsForValue().get(cacheKey)
                .flatMap(cached -> Mono.fromSupplier(() -> parse(cached)))
                .filter(vector -> vector.length == provider.dimension())
                .switchIfEmpty(Mono.defer(() -> provider.embed(text)
                        .flatMap(vector -> redis.opsForValue()
                                .set(cacheKey, serialize(vector), Duration.ofDays(30))
                                .onErrorResume(redisError -> Mono.just(Boolean.FALSE))
                                .thenReturn(vector))))
                .onErrorResume(error -> {
                    log.warn("Embedding cache/provider loi: {}", error.getMessage());
                    return provider.embed(text);
                });
    }

    private static String serialize(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 10);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    private static float[] parse(String value) {
        if (value == null || value.isBlank()) {
            return new float[0];
        }
        String[] parts = value.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
