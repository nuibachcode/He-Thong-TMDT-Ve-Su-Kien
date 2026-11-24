package vn.edu.tmdt.ai;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class GeminiEmbeddingClient implements EmbeddingClient {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";

    record Embedding(List<Float> values) {
    }

    record Response(Embedding embedding) {
    }

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private final int dimension;

    public GeminiEmbeddingClient(RestClient.Builder builder, String apiKey, String model, int dimension) {
        this.http = builder.baseUrl(BASE).build();
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
    }

    @Override
    public String provider() {
        return "gemini";
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Thieu GEMINI_API_KEY");
        }
        Map<String, Object> body = Map.of(
                "model", "models/" + model,
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", dimension);

        Response response = http.post()
                .uri("/models/{model}:embedContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Response.class);

        List<Float> values = response == null || response.embedding() == null
                ? null : response.embedding().values();
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("Gemini tra ve embedding rong");
        }
        float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }
}
