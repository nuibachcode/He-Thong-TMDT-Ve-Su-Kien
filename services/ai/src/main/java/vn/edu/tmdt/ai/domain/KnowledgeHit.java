package vn.edu.tmdt.ai.domain;

public record KnowledgeHit(
        Long id,
        String sourceType,
        Long sourceId,
        String title,
        String url,
        String content,
        double score) {
}
