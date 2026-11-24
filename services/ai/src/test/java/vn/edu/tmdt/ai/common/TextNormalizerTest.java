package vn.edu.tmdt.ai.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void foldsVietnameseDiacritics() {
        assertThat(TextNormalizer.fold("Thời tiết Hà Nội")).isEqualTo("thoi tiet ha noi");
        assertThat(TextNormalizer.fold("Đà Lạt 3N2Đ")).isEqualTo("da lat 3n2d");
    }

    @Test
    void tokenizesWithoutShortTokens() {
        assertThat(TextNormalizer.tokens("Tour Đà Lạt 3 ngày 2 đêm"))
                .contains("tour", "da", "lat", "ngay", "dem")
                .doesNotContain("3");
    }
}
