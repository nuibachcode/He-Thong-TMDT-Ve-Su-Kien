package vn.edu.tmdt.ai.domain;

import java.util.List;

public record Citation(String sourceType, Long sourceId, String title, String url, double score) {
}
