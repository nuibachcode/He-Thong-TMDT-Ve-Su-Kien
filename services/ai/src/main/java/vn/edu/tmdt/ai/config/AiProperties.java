package vn.edu.tmdt.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String provider,
        Integer embeddingDimension,
        Boolean reindexOnStart,
        Gemini gemini,
        Rag rag,
        Cache cache,
        RateLimit rateLimit) {

    public AiProperties {
        provider = provider == null || provider.isBlank() ? "mock" : provider.toLowerCase();
        embeddingDimension = embeddingDimension == null ? 768 : embeddingDimension;
        reindexOnStart = reindexOnStart != null && reindexOnStart;
        gemini = gemini == null ? new Gemini(null, null, null) : gemini;
        rag = rag == null ? new Rag(5, 0.55, 0.12, 800) : rag;
        cache = cache == null ? new Cache(true, 900) : cache;
        rateLimit = rateLimit == null ? new RateLimit(true, 20) : rateLimit;
    }

    public boolean geminiEnabled() {
        return "gemini".equals(provider)
                && gemini.apiKey() != null && !gemini.apiKey().isBlank();
    }

    public record Gemini(String apiKey, String chatModel, String embeddingModel) {
    }

    public record Rag(Integer topK, Double geminiThreshold, Double lexicalThreshold, Integer maxOutputTokens) {
        public Rag {
            topK = topK == null ? 5 : topK;
            geminiThreshold = geminiThreshold == null ? 0.55 : geminiThreshold;
            lexicalThreshold = lexicalThreshold == null ? 0.12 : lexicalThreshold;
            maxOutputTokens = maxOutputTokens == null ? 800 : maxOutputTokens;
        }
    }

    public record Cache(Boolean enabled, Integer ttlSeconds) {
        public Cache {
            enabled = enabled == null || enabled;
            ttlSeconds = ttlSeconds == null ? 900 : ttlSeconds;
        }
    }

    public record RateLimit(Boolean enabled, Integer perMinute) {
        public RateLimit {
            enabled = enabled == null || enabled;
            perMinute = perMinute == null ? 20 : perMinute;
        }
    }
}
