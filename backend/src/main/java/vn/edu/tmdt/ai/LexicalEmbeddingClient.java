package vn.edu.tmdt.ai;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Embedding "tu khoa" khi khong co API key / het quota: van tim kiem duoc, demo khong chet.
 * Khong phai semantic embedding - ghi ro trong bao cao.
 */
public class LexicalEmbeddingClient implements EmbeddingClient {

    private static final int DIM = 768;

    @Override
    public String provider() {
        return "lexical";
    }

    @Override
    public int dimension() {
        return DIM;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (String token : normalize(text).split("\\s+")) {
            if (token.length() < 2) {
                continue;
            }
            int h = token.hashCode();
            vector[Math.floorMod(h, DIM)] += 1f;
            vector[Math.floorMod(h * 31, DIM)] += 0.5f;
        }
        double norm = 0;
        for (float v : vector) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIM; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    private String normalize(String text) {
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim();
    }
}
