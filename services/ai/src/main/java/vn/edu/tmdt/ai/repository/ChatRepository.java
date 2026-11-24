package vn.edu.tmdt.ai.repository;

import java.time.OffsetDateTime;
import java.util.List;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.domain.ChatMessageRow;
import vn.edu.tmdt.ai.domain.ChatSessionRow;

@Repository
public class ChatRepository {

    private final DatabaseClient db;

    public ChatRepository(DatabaseClient db) {
        this.db = db;
    }

    public Mono<Long> createSession(Long userId, String guestKey) {
        GenericExecuteSpec spec = db.sql("""
                INSERT INTO chat_sessions (user_id, guest_key, last_message_at)
                VALUES (:userId, :guestKey, :now)
                RETURNING id
                """)
                .bind("now", OffsetDateTime.now());
        spec = userId == null ? spec.bindNull("userId", Long.class) : spec.bind("userId", userId);
        spec = guestKey == null || guestKey.isBlank()
                ? spec.bindNull("guestKey", String.class)
                : spec.bind("guestKey", guestKey);
        return spec.map((row, meta) -> row.get("id", Long.class)).one();
    }

    public Mono<ChatSessionRow> findSession(Long sessionId) {
        return db.sql("SELECT id, user_id, guest_key FROM chat_sessions WHERE id = :id")
                .bind("id", sessionId)
                .map((row, meta) -> new ChatSessionRow(
                        row.get("id", Long.class),
                        row.get("user_id", Long.class),
                        row.get("guest_key", String.class)))
                .one();
    }

    public Mono<Long> addMessage(Long sessionId, String role, String content, String citations,
                                 String provider, Integer latencyMs, boolean fallback, boolean refused) {
        GenericExecuteSpec spec = db.sql("""
                INSERT INTO chat_messages
                    (session_id, role, content, citations, provider, latency_ms, fallback, refused, created_at)
                VALUES
                    (:sessionId, :role, :content, :citations, :provider, :latencyMs, :fallback, :refused, :createdAt)
                RETURNING id
                """)
                .bind("sessionId", sessionId)
                .bind("role", role)
                .bind("content", content)
                .bind("provider", provider == null ? "" : provider)
                .bind("latencyMs", latencyMs == null ? 0 : latencyMs)
                .bind("fallback", fallback)
                .bind("refused", refused)
                .bind("createdAt", OffsetDateTime.now());
        spec = citations == null ? spec.bindNull("citations", String.class) : spec.bind("citations", citations);
        return spec.map((row, meta) -> row.get("id", Long.class)).one();
    }

    public Flux<ChatMessageRow> history(Long sessionId) {
        return db.sql("""
                SELECT id, session_id, role, content, citations, provider, latency_ms, fallback, refused, created_at
                FROM chat_messages
                WHERE session_id = :sessionId
                ORDER BY created_at ASC, id ASC
                """)
                .bind("sessionId", sessionId)
                .map((row, meta) -> map(row))
                .all();
    }

    public Flux<ChatMessageRow> recent(Long sessionId, int limit) {
        return db.sql("""
                SELECT * FROM (
                    SELECT id, session_id, role, content, citations, provider, latency_ms, fallback, refused, created_at
                    FROM chat_messages
                    WHERE session_id = :sessionId
                    ORDER BY created_at DESC, id DESC
                    LIMIT :limit
                ) recent
                ORDER BY created_at ASC, id ASC
                """)
                .bind("sessionId", sessionId)
                .bind("limit", limit)
                .map((row, meta) -> map(row))
                .all();
    }

    public Mono<Void> touchSession(Long sessionId) {
        return db.sql("UPDATE chat_sessions SET last_message_at = :now WHERE id = :id")
                .bind("now", OffsetDateTime.now())
                .bind("id", sessionId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    public Mono<Void> addFeedback(Long messageId, Long userId, int rating, String comment) {
        GenericExecuteSpec spec = db.sql("""
                INSERT INTO ai_feedback (message_id, user_id, rating, comment, created_at)
                VALUES (:messageId, :userId, :rating, :comment, :createdAt)
                """)
                .bind("messageId", messageId)
                .bind("rating", rating)
                .bind("createdAt", OffsetDateTime.now());
        spec = userId == null ? spec.bindNull("userId", Long.class) : spec.bind("userId", userId);
        spec = comment == null || comment.isBlank()
                ? spec.bindNull("comment", String.class) : spec.bind("comment", comment);
        return spec.fetch().rowsUpdated().then();
    }

    public Mono<AiMetrics> metrics() {
        return db.sql("""
                SELECT
                    (SELECT count(*) FROM chat_messages WHERE role = 'USER') AS questions,
                    (SELECT count(*) FROM chat_messages WHERE role = 'ASSISTANT') AS answers,
                    (SELECT count(*) FROM chat_messages WHERE role = 'ASSISTANT' AND fallback) AS fallback,
                    (SELECT count(*) FROM chat_messages WHERE role = 'ASSISTANT' AND refused) AS refused,
                    (SELECT coalesce(avg(latency_ms), 0) FROM chat_messages WHERE role = 'ASSISTANT') AS avg_latency,
                    (SELECT coalesce(avg(rating), 0) FROM ai_feedback) AS feedback_avg,
                    (SELECT count(*) FROM ai_feedback WHERE rating = 1) AS positive,
                    (SELECT count(*) FROM ai_feedback WHERE rating = -1) AS negative
                """)
                .map((row, meta) -> new AiMetrics(
                        row.get("questions", Long.class),
                        row.get("answers", Long.class),
                        row.get("fallback", Long.class),
                        row.get("refused", Long.class),
                        Math.round(number(row.get("avg_latency")) * 10) / 10.0,
                        Math.round(number(row.get("feedback_avg")) * 100) / 100.0,
                        row.get("positive", Long.class),
                        row.get("negative", Long.class)))
                .one();
    }

    private static ChatMessageRow map(Row row) {
        return new ChatMessageRow(
                row.get("id", Long.class),
                row.get("session_id", Long.class),
                row.get("role", String.class),
                row.get("content", String.class),
                row.get("citations", String.class),
                row.get("provider", String.class),
                row.get("latency_ms", Integer.class),
                Boolean.TRUE.equals(row.get("fallback", Boolean.class)),
                Boolean.TRUE.equals(row.get("refused", Boolean.class)),
                row.get("created_at", OffsetDateTime.class));
    }

    private static double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }

    public record AiMetrics(long questions, long answers, long fallback, long refused,
                            double avgLatencyMs, double feedbackAvg, long positive, long negative) {
    }
}
