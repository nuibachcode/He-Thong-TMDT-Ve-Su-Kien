package vn.edu.tmdt.ai;

import java.util.ArrayList;
import java.util.List;

public final class Chunker {

    private static final int MAX_CHARS = 1400;
    private static final int OVERLAP_CHARS = 200;

    private Chunker() {
    }

    public record Chunk(int index, String content) {
    }

    public static List<Chunk> split(String text) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();
        int index = 0;

        for (String paragraph : paragraphs) {
            String clean = paragraph.trim();
            if (clean.isEmpty()) {
                continue;
            }
            if (current.length() + clean.length() > MAX_CHARS && current.length() > 0) {
                chunks.add(new Chunk(index++, current.toString().trim()));
                String tail = current.substring(Math.max(0, current.length() - OVERLAP_CHARS));
                current = new StringBuilder(tail).append("\n\n");
            }
            current.append(clean).append("\n\n");
        }
        if (!current.isEmpty()) {
            chunks.add(new Chunk(index, current.toString().trim()));
        }
        return chunks;
    }
}
