package vn.edu.tmdt.ai;

public interface EmbeddingClient {

    String provider();

    int dimension();

    float[] embed(String text);
}
