package vn.edu.tmdt.ai.common;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Chuan hoa tieng Viet khong dau de guardrail va tim kiem tu khoa on dinh
 * voi ca cau hoi co dau lan khong dau.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String fold(String text) {
        if (text == null) {
            return "";
        }
        String value = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static List<String> tokens(String text) {
        return Arrays.stream(fold(text).split("\\s+"))
                .filter(token -> token.length() >= 2)
                .distinct()
                .toList();
    }
}
