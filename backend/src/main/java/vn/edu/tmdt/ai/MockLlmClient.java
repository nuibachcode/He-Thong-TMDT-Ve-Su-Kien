package vn.edu.tmdt.ai;

/** Khong goi mang: RagService se tu tra loi bang extractive tu chunk. */
public class MockLlmClient implements LlmClient {

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return "";
    }
}
