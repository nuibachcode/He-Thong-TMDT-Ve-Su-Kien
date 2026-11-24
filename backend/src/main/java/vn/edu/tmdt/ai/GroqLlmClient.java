package vn.edu.tmdt.ai;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class GroqLlmClient implements LlmClient {

    private static final String BASE = "https://api.groq.com/openai/v1";

    record Message(String role, String content) {
    }

    record Choice(Message message) {
    }

    record Response(List<Choice> choices) {
    }

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private final int maxOutputTokens;

    public GroqLlmClient(RestClient.Builder builder, String apiKey, String model, int maxOutputTokens) {
        this.http = builder.baseUrl(BASE).build();
        this.apiKey = apiKey;
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
    }

    @Override
    public String provider() {
        return "groq";
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "max_tokens", maxOutputTokens,
                "messages", List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userPrompt)));

        Response response = http.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Response.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "";
        }
        Message message = response.choices().get(0).message();
        return message == null ? "" : message.content();
    }
}
