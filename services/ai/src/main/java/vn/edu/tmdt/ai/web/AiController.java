package vn.edu.tmdt.ai.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.common.ApiException;
import vn.edu.tmdt.ai.common.ApiResponse;
import vn.edu.tmdt.ai.config.AiProperties;
import vn.edu.tmdt.ai.domain.Answer;
import vn.edu.tmdt.ai.domain.ChatMessageRow;
import vn.edu.tmdt.ai.domain.Recommendation;
import vn.edu.tmdt.ai.ingest.IngestService;
import vn.edu.tmdt.ai.rag.RagService;
import vn.edu.tmdt.ai.recommendation.RecommendationService;
import vn.edu.tmdt.ai.repository.ChatRepository;
import vn.edu.tmdt.ai.repository.ChatRepository.AiMetrics;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final RagService rag;
    private final RecommendationService recommendations;
    private final ChatRepository chat;
    private final IngestService ingest;
    private final AiProperties properties;

    public AiController(RagService rag, RecommendationService recommendations,
                        ChatRepository chat, IngestService ingest, AiProperties properties) {
        this.rag = rag;
        this.recommendations = recommendations;
        this.chat = chat;
        this.ingest = ingest;
        this.properties = properties;
    }

    public record ChatRequest(Long sessionId, String message) {
    }

    public record FeedbackRequest(@NotNull Long messageId, @NotNull @Min(-1) @Max(1) Integer rating,
                                  String comment) {
    }

    public record InterestsRequest(List<String> interests) {
    }

    public record MessageView(Long id, String role, String content, OffsetDateTime createdAt) {
    }

    @PostMapping("/chat")
    public Mono<ApiResponse<Answer>> chat(@Valid @RequestBody ChatRequest request,
                                          @RequestHeader(value = "X-User-Id", required = false) String userId,
                                          @RequestHeader(value = "X-Guest-Key", required = false) String guestKey) {
        return rag.ask(request.sessionId(), request.message(), parseUserId(userId), guestKey)
                .map(ApiResponse::ok);
    }

    @GetMapping("/chat/{sessionId}/messages")
    public Mono<ApiResponse<List<MessageView>>> history(@PathVariable Long sessionId,
                                                        @RequestHeader(value = "X-User-Id", required = false) String userId,
                                                        @RequestHeader(value = "X-Guest-Key", required = false) String guestKey) {
        return rag.history(sessionId, parseUserId(userId), guestKey)
                .map(rows -> ApiResponse.ok(rows.stream().map(this::toView).toList()));
    }

    @PostMapping("/feedback")
    public Mono<ApiResponse<String>> feedback(@Valid @RequestBody FeedbackRequest request,
                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return rag.feedback(request.messageId(), parseUserId(userId), request.rating(), request.comment())
                .thenReturn(ApiResponse.ok("Cam on ban da danh gia", "ok"));
    }

    @GetMapping("/recommendations")
    public Mono<ApiResponse<List<Recommendation>>> recommendations(
            @RequestParam(defaultValue = "4") int limit,
            @RequestParam(required = false) String interests,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return recommendations.recommend(parseUserId(userId), parseInterests(interests), limit)
                .map(ApiResponse::ok);
    }

    @GetMapping("/interests")
    public Mono<ApiResponse<List<String>>> interests(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Long id = requireUser(userId);
        return recommendations.savedInterests(id).map(ApiResponse::ok);
    }

    @PutMapping("/interests")
    public Mono<ApiResponse<List<String>>> saveInterests(
            @RequestBody InterestsRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Long id = requireUser(userId);
        return recommendations.saveInterests(id, request.interests()).map(ApiResponse::ok);
    }

    @PostMapping("/reindex")
    public Mono<ApiResponse<Map<String, Object>>> reindex(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.error(ApiException.forbidden("Chi ADMIN duoc reindex"));
        }
        return ingest.reindexAll()
                .map(count -> ApiResponse.ok("Da reindex " + count + " chunk",
                        Map.of("chunks", count, "provider", ingest.provider())));
    }

    @GetMapping("/metrics")
    public Mono<ApiResponse<AiMetrics>> metrics(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.error(ApiException.forbidden("Chi ADMIN duoc xem metrics"));
        }
        return chat.metrics().map(ApiResponse::ok);
    }

    @GetMapping("/health")
    public Mono<ApiResponse<Map<String, Object>>> health() {
        return ingest.count().map(count -> ApiResponse.ok(Map.of(
                "status", "UP",
                "app", "ai-service",
                "provider", ingest.provider(),
                "chunks", count,
                "geminiConfigured", properties.geminiEnabled())));
    }

    private MessageView toView(ChatMessageRow message) {
        return new MessageView(message.id(), message.role().toLowerCase(), message.content(),
                message.createdAt());
    }

    private Long requireUser(String userId) {
        Long id = parseUserId(userId);
        if (id == null) {
            throw ApiException.unauthorized("Ban can dang nhap de luu so thich");
        }
        return id;
    }

    private Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> parseInterests(String interests) {
        if (interests == null || interests.isBlank()) {
            return List.of();
        }
        return Arrays.stream(interests.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(10)
                .toList();
    }
}
