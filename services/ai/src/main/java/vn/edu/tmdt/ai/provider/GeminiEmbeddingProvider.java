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

/**
 * Gemini embedding API: models/gemini-embedding-001, 768 chieu.
 */
@Component
public class GeminiEmbeddingProvider implements EmbeddingProvider {

    private final WebClient http;
    private final AiProperties properties;

    public GeminiEmbeddingProvider(WebClient.Builder builder, AiProperties properties) {
        this.http = builder.baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
        this.properties = properties;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public int dimension() {
        return properties.embeddingDimension();
    }

    @Override
    public Mono<float[]> embed(String text) {
        Map<String, Object> body = Map.of(
                "model", "models/" + properties.gemini().embeddingModel(),
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", dimension());

        return http.post()
                .uri("/models/{model}:embedContent", properties.gemini().embeddingModel())
                .header("x-goog-api-key", properties.gemini().apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(30))
                .map(this::parse);
    }

    private float[] parse(JsonNode response) {
        JsonNode values = response == null ? null : response.path("embedding").path("values");
        if (values == null || !values.isArray() || values.isEmpty()) {
            throw new IllegalStateException("Gemini tra ve embedding rong");
        }
        float[] vector = new float[values.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) values.get(i).asDouble();
        }
        return vector;
    }
}
