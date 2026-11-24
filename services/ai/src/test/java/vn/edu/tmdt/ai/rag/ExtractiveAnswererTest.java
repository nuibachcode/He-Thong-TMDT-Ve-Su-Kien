package vn.edu.tmdt.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import vn.edu.tmdt.ai.domain.KnowledgeHit;

class ExtractiveAnswererTest {

    private final ExtractiveAnswerer answerer = new ExtractiveAnswerer();

    @Test
    void picksRefundRuleForPolicyQuestion() {
        KnowledgeHit policy = new KnowledgeHit(1L, "POLICY", 1L, "Chính sách hoàn và hủy vé, tour", null,
                """
                Chính sách: Chính sách hoàn và hủy vé, tour
                - Hủy trước ngày khởi hành từ 7 ngày trở lên: hoàn 100% giá trị tour.
                - Hủy trước ngày khởi hành từ 3 đến 6 ngày: hoàn 50% giá trị tour.
                - Hủy trước ngày khởi hành dưới 72 giờ: không hoàn tiền.
                Từ khóa: hoàn tiền, hủy tour
                """, 0.9);

        String answer = answerer.answer(List.of(policy), "Huỷ tour trước 5 ngày có được hoàn tiền không?");

        assertThat(answer).contains("50%");
        assertThat(answer).doesNotContain("Từ khóa:");
    }

    @Test
    void picksExclusionForTourQuestion() {
        KnowledgeHit tour = new KnowledgeHit(2L, "TOUR", 3L, "Tour Phú Quốc 4N3Đ", "/tours/phu-quoc",
                """
                Tour: Tour Phú Quốc 4N3Đ
                Mô tả: Tour Phú Quốc 4 ngày 3 đêm kết hợp nghỉ dưỡng và khám phá biển.
                Bao gồm: khách sạn 3 sao, ăn sáng buffet, xe đưa đón sân bay.
                Không bao gồm: vé máy bay khứ hồi, bữa trưa, bữa tối và chi phí cá nhân.
                Từ khóa: phú quốc, biển đảo
                """, 0.8);

        String answer = answerer.answer(List.of(tour), "Tour Phú Quốc có bao gồm vé máy bay không?");

        assertThat(answer).contains("Không bao gồm");
        assertThat(answer).contains("vé máy bay");
    }
}
