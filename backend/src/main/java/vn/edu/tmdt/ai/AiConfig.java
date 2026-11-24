package vn.edu.tmdt.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Chon provider bang bien moi truong AI_PROVIDER: mock | gemini | groq.
 * Khong co key -> tu dong dung mock/lexical de he thong van chay.
 */
@Configuration
public class AiConfig {

    /** Spring Boot 4 khong con auto-configure RestClient.Builder, tu khai bao bean. */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public EmbeddingClient embeddingClient(
            RestClient.Builder builder,
            @Value("${ai.provider:mock}") String provider,
            @Value("${ai.gemini.api-key:}") String geminiKey,
            @Value("${ai.gemini.embedding-model:gemini-embedding-001}") String embeddingModel,
            @Value("${ai.embedding-dimension:768}") int dimension) {

        if ("gemini".equalsIgnoreCase(provider) && geminiKey != null && !geminiKey.isBlank()) {
            return new GeminiEmbeddingClient(builder, geminiKey, embeddingModel, dimension);
        }
        return new LexicalEmbeddingClient();
    }

    @Bean
    public LlmClient llmClient(
            RestClient.Builder builder,
            @Value("${ai.provider:mock}") String provider,
            @Value("${ai.gemini.api-key:}") String geminiKey,
            @Value("${ai.gemini.chat-model:gemini-2.5-flash}") String geminiModel,
            @Value("${ai.groq.api-key:}") String groqKey,
            @Value("${ai.groq.chat-model:llama-3.3-70b-versatile}") String groqModel,
            @Value("${ai.rag.max-output-tokens:800}") int maxOutputTokens) {

        if ("gemini".equalsIgnoreCase(provider) && geminiKey != null && !geminiKey.isBlank()) {
            return new GeminiLlmClient(builder, geminiKey, geminiModel, maxOutputTokens);
        }
        if ("groq".equalsIgnoreCase(provider) && groqKey != null && !groqKey.isBlank()) {
            return new GroqLlmClient(builder, groqKey, groqModel, maxOutputTokens);
        }
        return new MockLlmClient();
    }
}
