package vn.edu.tmdt.ai.provider;

import reactor.core.publisher.Mono;

public interface EmbeddingProvider {

    String name();

    int dimension();

    Mono<float[]> embed(String text);
}
