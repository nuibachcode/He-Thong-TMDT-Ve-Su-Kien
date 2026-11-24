package vn.edu.tmdt.ai;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class GeminiLlmClient implements LlmClient {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";

    record Part(String text) {
    }

    record Content(List<Part> parts) {
    }

    record Candidate(Content content) {
    }

    record Response(List<Candidate> candidates) {
    }

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    public GeminiLlmClient(RestClient.Builder builder, String apiKey, String model, int maxOutputTokens) {
        this.http = builder.baseUrl(BASE).build();
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
    }

    @Override
    public String provider() {
        return "gemini";
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of("temperature", 0.2, "maxOutputTokens", maxOutputTokens));

        Response response = http.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Response.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return "";
        }
        List<Part> parts = response.candidates().get(0).content().parts();
        return parts == null || parts.isEmpty() ? "" : parts.get(0).text();
    }
}
