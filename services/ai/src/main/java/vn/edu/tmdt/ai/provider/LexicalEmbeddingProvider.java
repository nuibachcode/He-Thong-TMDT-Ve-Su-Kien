package vn.edu.tmdt.ai.provider;

import java.util.Locale;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.common.TextNormalizer;

/**
 * Embedding "tu khoa" (hashing) khi khong co API key hoac het quota.
 * Khong phai semantic embedding - dung cho demo offline va du phong.
 */
@Component
public class LexicalEmbeddingProvider implements EmbeddingProvider {

    private final int dimension;

    public LexicalEmbeddingProvider(vn.edu.tmdt.ai.config.AiProperties properties) {
        this.dimension = properties.embeddingDimension();
    }

    @Override
    public String name() {
        return "lexical";
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public Mono<float[]> embed(String text) {
        return Mono.fromSupplier(() -> build(text));
    }

    private float[] build(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (String token : TextNormalizer.fold(text).split("\\s+")) {
            if (token.length() < 2) {
                continue;
            }
            int hash = token.hashCode();
            vector[Math.floorMod(hash, dimension)] += 1f;
            vector[Math.floorMod(hash * 31, dimension)] += 0.5f;
        }
        double norm = 0;
        for (float value : vector) {
            norm += (double) value * value;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dimension; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    static String describe(float[] values) {
        StringBuilder sb = new StringBuilder(values.length * 10);
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.7f", values[i]));
        }
        return sb.toString();
    }
}
