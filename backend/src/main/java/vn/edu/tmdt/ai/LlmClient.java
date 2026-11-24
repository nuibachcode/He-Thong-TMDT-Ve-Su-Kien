package vn.edu.tmdt.ai;

public interface LlmClient {

    String provider();

    /** Tra ve null/rong neu provider khong san sang (mock) de tang tren dung extractive. */
    String generate(String systemPrompt, String userPrompt);
}
