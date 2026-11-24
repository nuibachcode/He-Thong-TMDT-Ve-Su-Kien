package vn.edu.tmdt.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import vn.edu.tmdt.ai.config.AiProperties;

@Component
public class GeminiLlmProvider {

    private final WebClient http;
    private final AiProperties properties;

    public GeminiLlmProvider(WebClient.Builder builder, AiProperties properties) {
        this.http = builder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
        this.properties = properties;
    }

    public String name() {
        return "gemini";
    }

    public Mono<String> generate(String system, String prompt) {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", properties.rag().maxOutputTokens()));

        return http.post()
                .uri("/models/{model}:generateContent", properties.gemini().chatModel())
                .header("x-goog-api-key", properties.gemini().apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(45))
                .map(this::parse);
    }

    private String parse(JsonNode response) {
        JsonNode parts = response == null
                ? null
                : response.path("candidates").path(0).path("content").path("parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(text.trim());
            }
        }
        return sb.toString();
    }
}
