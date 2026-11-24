package vn.edu.tmdt.ai.domain;

import java.time.OffsetDateTime;

public record ChatMessageRow(
        Long id,
        Long sessionId,
        String role,
        String content,
        String citations,
        String provider,
        Integer latencyMs,
        boolean fallback,
        boolean refused,
        OffsetDateTime createdAt) {
}
