package vn.edu.tmdt.ai.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import vn.edu.tmdt.ai.common.TextNormalizer;
import vn.edu.tmdt.ai.domain.KnowledgeHit;

/**
 * Tra loi trich xuat tu ngu canh: chon cau/doan co nhieu tu khoa cua cau hoi nhat.
 * Dung khi AI_PROVIDER=mock, het quota hoac LLM loi.
 */
@Component
public class ExtractiveAnswerer {

    private static final Set<String> GENERIC = Set.of(
            "tour", "ve", "va", "co", "cua", "cho", "khi", "thi", "la", "cac", "nhung",
            "mot", "nay", "hom", "the", "nao", "gi", "dau", "ai", "khong", "duoc",
            "voi", "hay", "hoac", "neu", "may", "loai", "nhe", "giup", "toi", "minh",
            "ban", "can", "muon", "biet", "hoi", "do", "kia", "rang", "de", "tai",
            "trong", "ngoai", "cung", "rat", "cach", "ra", "sao", "nhieu", "bao");

    private static final List<String> HEADER_PREFIXES = List.of(
            "chính sách:", "tour:", "sự kiện:", "thời lượng:", "khởi hành từ:",
            "điểm đến:", "giá cơ bản:", "giá vé từ:", "địa điểm:", "danh mục:",
            "thời gian:", "từ khóa:", "bao gồm:", "không bao gồm:", "điểm nhấn:",
            "mô tả:", "quy định chung");

    public String answer(List<KnowledgeHit> hits, String question) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        List<KnowledgeHit> chosen = new ArrayList<>();
        KnowledgeHit top = hits.get(0);
        chosen.add(top);

        if (!"POLICY".equals(top.sourceType())) {
            for (KnowledgeHit hit : hits.subList(1, hits.size())) {
                if (Objects.equals(hit.sourceId(), top.sourceId())) {
                    continue;
                }
                if (hit.sourceType().equals(top.sourceType())) {
                    chosen.add(hit);
                    break;
                }
            }
        }

        StringBuilder answer = new StringBuilder("Theo dữ liệu hệ thống:");
        for (KnowledgeHit hit : chosen) {
            String excerpt = bestExcerpt(hit.content(), question);
            if (!excerpt.isBlank()) {
                answer.append("\n• ").append(hit.title()).append(": ").append(excerpt);
            }
        }
        return answer.toString();
    }

    private String bestExcerpt(String content, String question) {
        List<String> keywords = TextNormalizer.tokens(question).stream()
                .filter(token -> !GENERIC.contains(token))
                .toList();

        List<String> candidates = new ArrayList<>();
        for (String raw : content.split("(?<=[.!?])\\s+|\\n+")) {
            String line = raw.trim().replaceFirst("^[-•*]\\s*", "");
            String folded = TextNormalizer.fold(line);
            if (line.length() < 25 || folded.length() < 20) {
                continue;
            }
            String lower = line.toLowerCase(java.util.Locale.ROOT);
            boolean header = HEADER_PREFIXES.stream().anyMatch(lower::startsWith);
            if (header) {
                continue;
            }
            candidates.add(line);
        }
        if (candidates.isEmpty()) {
            String clean = content.trim();
            return clean.substring(0, Math.min(320, clean.length()));
        }

        List<Scored> scored = new ArrayList<>();
        for (String candidate : candidates) {
            String folded = TextNormalizer.fold(candidate);
            int score = 0;
            for (String keyword : keywords) {
                if (folded.contains(keyword)) {
                    score++;
                }
            }
            scored.add(new Scored(candidate, score));
        }
        scored.sort((a, b) -> {
            if (a.score != b.score) {
                return Integer.compare(b.score, a.score);
            }
            return Integer.compare(b.text.length(), a.text.length());
        });

        LinkedHashSet<String> picked = new LinkedHashSet<>();
        int total = 0;
        for (Scored item : scored) {
            if (item.score < 1 && !picked.isEmpty()) {
                break;
            }
            if (total + item.text.length() > 380) {
                continue;
            }
            picked.add(item.text);
            total += item.text.length();
            if (picked.size() >= 3) {
                break;
            }
        }
        return String.join(" ", picked);
    }

    private record Scored(String text, int score) {
    }
}
