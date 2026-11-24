package vn.edu.tmdt.ai.domain;

import java.util.List;

public record Answer(
        Long sessionId,
        Long messageId,
        String answer,
        List<Citation> citations,
        String provider,
        long latencyMs,
        boolean refused,
        boolean fallback) {
}
