package vn.edu.tmdt.ai.domain;

import java.util.List;

public record Recommendation(
        String type,
        Long id,
        String title,
        String slug,
        String subtitle,
        long price,
        String coverImageUrl,
        String reason,
        String url,
        List<String> tags) {
}
