package vn.edu.tmdt.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.cache.AnswerCache;
import vn.edu.tmdt.ai.cache.AnswerCache.CachedAnswer;
import vn.edu.tmdt.ai.cache.RateLimiter;
import vn.edu.tmdt.ai.common.ApiException;
import vn.edu.tmdt.ai.common.TextNormalizer;
import vn.edu.tmdt.ai.config.AiProperties;
import vn.edu.tmdt.ai.domain.Answer;
import vn.edu.tmdt.ai.domain.ChatMessageRow;
import vn.edu.tmdt.ai.domain.ChatSessionRow;
import vn.edu.tmdt.ai.domain.Citation;
import vn.edu.tmdt.ai.domain.KnowledgeHit;
import vn.edu.tmdt.ai.provider.EmbeddingRouter;
import vn.edu.tmdt.ai.provider.GeminiLlmProvider;
import vn.edu.tmdt.ai.repository.ChatRepository;
import vn.edu.tmdt.ai.repository.KnowledgeRepository;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String REFUSAL = "Mình chưa có thông tin về nội dung này trong hệ thống. "
            + "Bạn có thể xem danh sách tour đang mở bán hoặc liên hệ hỗ trợ 1900 0000 nhé.";

    private static final List<String> OFF_DOMAIN = List.of(
            "thoi tiet", "gia vang", "ty gia", "chung khoan", "bong da", "tin tuc",
            "thoi su", "bau cu", "bitcoin", "crypto", "ngoai te", "chinh tri",
            "dien anh", "am nhac the gioi", "lich am");

    private static final Set<String> STOPWORDS = Set.of(
            "tour", "ve", "va", "co", "cua", "cho", "khi", "thi", "la", "cac", "nhung",
            "mot", "ngay", "dem", "gia", "bao", "nhieu", "nay", "hom", "the", "nao", "gi",
            "dau", "ai", "khong", "duoc", "voi", "hay", "hoac", "neu", "truoc", "sau",
            "may", "loai", "nhe", "giup", "toi", "minh", "ban", "em", "anh", "chi",
            "can", "muon", "biet", "hoi", "do", "kia", "rang", "de", "tai", "tu", "den",
            "trong", "ngoai", "cung", "rat", "cach", "ra", "sao");

    private static final Pattern MONEY = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+");
    private static final Pattern DATE = Pattern.compile("\\d{1,2}/\\d{1,2}(?:/\\d{4})?");

    private final AiProperties properties;
    private final EmbeddingRouter embedding;
    private final KnowledgeRepository knowledge;
    private final ChatRepository chat;
    private final GeminiLlmProvider geminiLlm;
    private final ExtractiveAnswerer extractive;
    private final AnswerCache answerCache;
    private final RateLimiter rateLimiter;
    private final ObjectMapper mapper;

    public RagService(AiProperties properties, EmbeddingRouter embedding, KnowledgeRepository knowledge,
                      ChatRepository chat, GeminiLlmProvider geminiLlm, ExtractiveAnswerer extractive,
                      AnswerCache answerCache, RateLimiter rateLimiter, ObjectMapper mapper) {
        this.properties = properties;
        this.embedding = embedding;
        this.knowledge = knowledge;
        this.chat = chat;
        this.geminiLlm = geminiLlm;
        this.extractive = extractive;
        this.answerCache = answerCache;
        this.rateLimiter = rateLimiter;
        this.mapper = mapper;
    }

    public Mono<Answer> ask(Long sessionId, String rawMessage, Long userId, String guestKey) {
        long start = System.currentTimeMillis();
        String question = rawMessage == null ? "" : rawMessage.trim();
        if (question.isEmpty()) {
            return Mono.error(ApiException.badRequest("Cau hoi khong duoc de trong"));
        }
        if (question.length() > 1000) {
            return Mono.error(ApiException.badRequest("Cau hoi qua dai (toi da 1000 ky tu)"));
        }

        return rateLimiter.check(identity(userId, guestKey))
                .then(resolveSession(sessionId, userId, guestKey))
                .flatMap(session -> chat.addMessage(session.id(), "USER", question, null, null, 0, false, false)
                        .then(chat.recent(session.id(), 8).collectList())
                        .flatMap(history -> compute(question, history))
                        .flatMap(computed -> saveAnswer(session, question, computed, start)));
    }

    public Mono<List<ChatMessageRow>> history(Long sessionId, Long userId, String guestKey) {
        return resolveSession(sessionId, userId, guestKey)
                .thenMany(chat.history(sessionId))
                .collectList();
    }

    public Mono<Void> feedback(Long messageId, Long userId, int rating, String comment) {
        if (rating != 1 && rating != -1) {
            return Mono.error(ApiException.badRequest("Danh gia chi nhan 1 hoac -1"));
        }
        return chat.addFeedback(messageId, userId, rating, comment);
    }

    private Mono<Computed> compute(String question, List<ChatMessageRow> history) {
        String llmProvider = llmProvider();
        String embeddingProvider = embedding.providerName();

        return answerCache.get(TextNormalizer.fold(question), embeddingProvider + ":" + llmProvider)
                .map(cached -> new Computed(cached.answer(), cached.citations(),
                        cached.refused(), cached.fallback(), llmProvider, true))
                .switchIfEmpty(Mono.defer(() -> retrieve(question, history, llmProvider, embeddingProvider)
                        .flatMap(computed -> answerCache.put(TextNormalizer.fold(question),
                                embeddingProvider + ":" + llmProvider,
                                new CachedAnswer(computed.answer(), computed.citations(),
                                        computed.refused(), computed.fallback()))
                                .thenReturn(computed))));
    }

    private Mono<Computed> retrieve(String question, List<ChatMessageRow> history,
                                    String llmProvider, String embeddingProvider) {
        if (isOffDomain(question)) {
            return Mono.just(refusal(llmProvider));
        }

        double threshold = "gemini".equals(embeddingProvider)
                ? properties.rag().geminiThreshold() : properties.rag().lexicalThreshold();

        return embedding.embed(question)
                .flatMapMany(vector -> knowledge.search(embeddingProvider, vector, threshold,
                        properties.rag().topK()))
                .collectList()
                .map(hits -> "lexical".equals(embeddingProvider) ? lexicalFilter(hits, question) : hits)
                .flatMap(hits -> {
                    log.info("RAG q='{}' embedding={} hits={}", question, embeddingProvider, hits.size());
                    if (hits.isEmpty()) {
                        return Mono.just(refusal(llmProvider));
                    }
                    List<Citation> citations = hits.stream()
                            .map(hit -> new Citation(hit.sourceType(), hit.sourceId(), hit.title(),
                                    hit.url(), Math.round(hit.score() * 1000) / 1000.0))
                            .toList();
                    String prompt = PromptBuilder.build(question, hits, history);
                    return generate(question, hits, prompt, llmProvider)
                            .map(answer -> new Computed(answer.answer(), citations,
                                    answer.refused(), answer.fallback(), llmProvider, false));
                });
    }

    private Mono<Generated> generate(String question, List<KnowledgeHit> hits, String prompt,
                                     String llmProvider) {
        if (!"gemini".equals(llmProvider)) {
            String answer = extractive.answer(hits, question);
            return Mono.just(new Generated(answer, false, false));
        }
        return geminiLlm.generate(PromptBuilder.SYSTEM, prompt)
                .map(answer -> {
                    if (answer == null || answer.isBlank() || !numbersGrounded(answer, hits)) {
                        return new Generated(extractive.answer(hits, question), true, false);
                    }
                    return new Generated(answer, false, containsRefusal(answer));
                })
                .onErrorResume(error -> {
                    log.warn("Gemini loi, dung extractive: {}", error.getMessage());
                    return Mono.just(new Generated(extractive.answer(hits, question), true, false));
                });
    }

    private Mono<Answer> saveAnswer(ChatSessionRow session, String question, Computed computed, long start) {
        long latency = System.currentTimeMillis() - start;
        String citationsJson;
        try {
            citationsJson = mapper.writeValueAsString(computed.citations());
        } catch (Exception ex) {
            citationsJson = "[]";
        }
        String finalCitations = citationsJson;
        return chat.addMessage(session.id(), "ASSISTANT", computed.answer(), finalCitations,
                        computed.provider(), (int) latency, computed.fallback(), computed.refused())
                .flatMap(messageId -> chat.touchSession(session.id())
                        .thenReturn(new Answer(session.id(), messageId, computed.answer(),
                                computed.citations(), computed.provider(), latency,
                                computed.refused(), computed.fallback())));
    }

    private Mono<ChatSessionRow> resolveSession(Long sessionId, Long userId, String guestKey) {
        if (sessionId == null) {
            return chat.createSession(userId, guestKey)
                    .map(id -> new ChatSessionRow(id, userId, guestKey));
        }
        return chat.findSession(sessionId)
                .switchIfEmpty(Mono.error(ApiException.notFound("Khong tim thay phien chat")))
                .flatMap(session -> {
                    if (session.userId() != null && !session.userId().equals(userId)) {
                        return Mono.error(ApiException.forbidden("Ban khong co quyen truy cap phien chat nay"));
                    }
                    if (session.userId() == null && session.guestKey() != null && guestKey != null
                            && !session.guestKey().equals(guestKey)) {
                        return Mono.error(ApiException.forbidden("Ban khong co quyen truy cap phien chat nay"));
                    }
                    return Mono.just(session);
                });
    }

    private List<KnowledgeHit> lexicalFilter(List<KnowledgeHit> hits, String question) {
        List<String> distinctive = TextNormalizer.tokens(question).stream()
                .filter(token -> !STOPWORDS.contains(token))
                .limit(8)
                .toList();
        if (distinctive.isEmpty()) {
            return hits;
        }
        return hits.stream()
                .filter(hit -> overlap(hit.content(), distinctive) >= 1)
                .toList();
    }

    private int overlap(String content, List<String> tokens) {
        String folded = TextNormalizer.fold(content);
        int count = 0;
        for (String token : tokens) {
            if (folded.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private boolean isOffDomain(String question) {
        String folded = TextNormalizer.fold(question);
        return OFF_DOMAIN.stream().anyMatch(folded::contains);
    }

    private boolean numbersGrounded(String answer, List<KnowledgeHit> hits) {
        StringBuilder context = new StringBuilder();
        for (KnowledgeHit hit : hits) {
            context.append(hit.content().replace(".", "")).append(' ');
        }
        String normalizedContext = context.toString();

        Matcher money = MONEY.matcher(answer);
        while (money.find()) {
            if (!normalizedContext.contains(money.group().replace(".", ""))) {
                return false;
            }
        }
        Matcher date = DATE.matcher(answer);
        while (date.find()) {
            if (!normalizedContext.contains(date.group())) {
                return false;
            }
        }
        return true;
    }

    private boolean containsRefusal(String answer) {
        String folded = TextNormalizer.fold(answer);
        return folded.contains("chua co thong tin") || folded.contains("khong co thong tin");
    }

    private String llmProvider() {
        return properties.geminiEnabled() ? "gemini" : "mock";
    }

    private String identity(Long userId, String guestKey) {
        if (userId != null) {
            return "u" + userId;
        }
        if (guestKey != null && !guestKey.isBlank()) {
            return "g" + guestKey;
        }
        return "anonymous";
    }

    private Computed refusal(String provider) {
        return new Computed(REFUSAL, List.of(), true, false, provider, false);
    }

    private record Generated(String answer, boolean fallback, boolean refused) {
    }

    private record Computed(String answer, List<Citation> citations, boolean refused, boolean fallback,
                            String provider, boolean cached) {
    }
}
