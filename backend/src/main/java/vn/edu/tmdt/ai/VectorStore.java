package vn.edu.tmdt.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Vector store "trong MySQL": nap chunk vao bo nho, tinh cosine.
 * Voi ~1000 chunk chi mat vai ms; doi sang Qdrant chi can thay class nay.
 */
@Component
public class VectorStore {

    public record Hit(long id, String sourceType, Long sourceId, String title, String content, double score) {
    }

    private record Entry(long id, String sourceType, Long sourceId, String title, String content, float[] vector) {
    }

    private final KnowledgeChunkRepository repository;
    private volatile List<Entry> index;

    public VectorStore(KnowledgeChunkRepository repository) {
        this.repository = repository;
    }

    public List<Hit> search(float[] query, int topK, String sourceType, double threshold) {
        List<Entry> entries = entries();
        List<Hit> hits = new ArrayList<>();
        for (Entry entry : entries) {
            if (sourceType != null && !sourceType.equals(entry.sourceType())) {
                continue;
            }
            double score = cosine(query, entry.vector());
            if (score >= threshold) {
                hits.add(new Hit(entry.id(), entry.sourceType(), entry.sourceId(), entry.title(),
                        entry.content(), score));
            }
        }
        hits.sort(Comparator.comparingDouble(Hit::score).reversed());
        return hits.size() > topK ? hits.subList(0, topK) : hits;
    }

    /** Goi sau khi ingest/xoa de lan tim kiem sau nap lai du lieu. */
    public void invalidate() {
        index = null;
    }

    private List<Entry> entries() {
        List<Entry> local = index;
        if (local == null) {
            local = new ArrayList<>();
            for (KnowledgeChunk chunk : repository.findAll()) {
                float[] vector = parse(chunk.embedding);
                if (vector.length > 0) {
                    local.add(new Entry(chunk.id, chunk.sourceType, chunk.sourceId, chunk.title,
                            chunk.content, vector));
                }
            }
            index = local;
        }
        return local;
    }

    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static String toJson(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 9);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    public static float[] parse(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        String body = json.trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.isBlank()) {
            return new float[0];
        }
        String[] parts = body.split(",");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = Float.parseFloat(parts[i].trim());
        }
        return out;
    }
}
