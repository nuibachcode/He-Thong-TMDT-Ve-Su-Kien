package vn.edu.tmdt.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.auth.UserRepository;
import vn.edu.tmdt.common.ApiResponse;
import vn.edu.tmdt.ai.RecommendationService.Recommendation;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final RagService rag;
    private final RecommendationService recommendations;
    private final AiFeedbackRepository feedback;
    private final UserRepository users;

    public AiController(RagService rag, RecommendationService recommendations,
                        AiFeedbackRepository feedback, UserRepository users) {
        this.rag = rag;
        this.recommendations = recommendations;
        this.feedback = feedback;
        this.users = users;
    }

    public record ChatRequest(Long sessionId, String message) {
    }

    public record FeedbackRequest(@NotNull Long messageId, @NotNull @Min(-1) @Max(1) Integer rating,
                                  String comment) {
    }

    public record MessageView(Long id, String role, String content, Instant createdAt) {
    }

    @PostMapping("/chat")
    public ApiResponse<RagService.Answer> chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
        Long userId = resolveUserId(auth);
        return ApiResponse.ok(rag.ask(request.sessionId(), request.message(), userId));
    }

    @GetMapping("/chat/{sessionId}/messages")
    public ApiResponse<List<MessageView>> history(@PathVariable Long sessionId) {
        return ApiResponse.ok(rag.history(sessionId).stream()
                .map(m -> new MessageView(m.id, m.role, m.content, m.createdAt)).toList());
    }

    @PostMapping("/feedback")
    public ApiResponse<String> feedback(@Valid @RequestBody FeedbackRequest request, Authentication auth) {
        AiFeedback entity = new AiFeedback();
        entity.messageId = request.messageId();
        entity.userId = resolveUserId(auth);
        entity.rating = request.rating();
        entity.comment = request.comment();
        feedback.save(entity);
        return ApiResponse.ok("Cam on ban da danh gia", "ok");
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<Recommendation>> recommendations(@RequestParam(defaultValue = "4") int limit,
                                                             Authentication auth) {
        User user = auth == null ? null
                : users.findByEmailIgnoreCase(auth.getName()).orElse(null);
        int safeLimit = Math.max(1, Math.min(limit, 12));
        return ApiResponse.ok(recommendations.recommend(user, safeLimit));
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return users.findByEmailIgnoreCase(auth.getName()).map(User::getId).orElse(null);
    }
}
