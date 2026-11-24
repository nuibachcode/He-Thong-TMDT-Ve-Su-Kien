package vn.edu.tmdt.ai.repository;

import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.domain.KnowledgeHit;

@Repository
public class KnowledgeRepository {

    private final DatabaseClient db;

    public KnowledgeRepository(DatabaseClient db) {
        this.db = db;
    }

    public Mono<Long> count() {
        return db.sql("SELECT count(*) AS total FROM knowledge_chunks")
                .map((row, meta) -> row.get("total", Long.class))
                .one();
    }

    public Mono<Long> countByProvider(String provider) {
        return db.sql("SELECT count(*) AS total FROM knowledge_chunks WHERE provider = :provider")
                .bind("provider", provider)
                .map((row, meta) -> row.get("total", Long.class))
                .one();
    }

    public Mono<Void> deleteAll() {
        return db.sql("DELETE FROM knowledge_chunks").then();
    }

    public Mono<Void> insert(String sourceType, Long sourceId, String title, String url,
                             String content, String contentHash, String provider, float[] embedding) {
        String vector = toVectorLiteral(embedding);
        String sql = """
                INSERT INTO knowledge_chunks
                    (source_type, source_id, title, url, content, content_hash, provider, embedding, updated_at)
                VALUES
                    (:sourceType, :sourceId, :title, :url, :content, :contentHash, :provider,
                     '%s'::vector, :updatedAt)
                """.formatted(vector);

        return db.sql(sql)
                .bind("sourceType", sourceType)
                .bind("sourceId", sourceId)
                .bind("title", title)
                .bind("url", url == null ? "" : url)
                .bind("content", content)
                .bind("contentHash", contentHash)
                .bind("provider", provider)
                .bind("updatedAt", OffsetDateTime.now())
                .fetch()
                .rowsUpdated()
                .then();
    }

    public Flux<KnowledgeHit> search(String provider, float[] embedding, double threshold, int limit) {
        String vector = toVectorLiteral(embedding);
        String sql = """
                SELECT id, source_type, source_id, title, url, content,
                       1 - (embedding <=> '%s'::vector) AS score
                FROM knowledge_chunks
                WHERE provider = :provider
                ORDER BY embedding <=> '%s'::vector
                LIMIT :limit
                """.formatted(vector, vector);

        return db.sql(sql)
                .bind("provider", provider)
                .bind("limit", limit)
                .map((row, meta) -> new KnowledgeHit(
                        row.get("id", Long.class),
                        row.get("source_type", String.class),
                        row.get("source_id", Long.class),
                        row.get("title", String.class),
                        row.get("url", String.class),
                        row.get("content", String.class),
                        number(row.get("score"))))
                .all()
                .filter(hit -> hit.score() >= threshold && hit.score() > 0);
    }

    private static double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }

    /** pgvector nhan literal dang '[0.1,0.2,...]'. Gia tri chi gom so do code sinh ra. */
    public static String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder(embedding.length * 10);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.ROOT, "%.7f", embedding[i]));
        }
        return sb.append(']').toString();
    }
}
