package vn.edu.tmdt.ai;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.catalog.Event;
import vn.edu.tmdt.catalog.EventRepository;
import vn.edu.tmdt.catalog.TicketType;
import vn.edu.tmdt.catalog.TicketTypeRepository;
import vn.edu.tmdt.catalog.Tour;
import vn.edu.tmdt.catalog.TourRepository;
import vn.edu.tmdt.catalog.TourSchedule;
import vn.edu.tmdt.catalog.TourScheduleRepository;
import vn.edu.tmdt.catalog.Venue;
import vn.edu.tmdt.catalog.VenueRepository;

/**
 * Bien du lieu nghiep vu thanh chunk + embedding de RAG truy xuat.
 * Chay lai toan bo khi khoi dong (du lieu demo nho), va tung phan khi CRUD.
 */
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONE);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONE);
    private static final NumberFormat MONEY = NumberFormat.getIntegerInstance(new Locale("vi", "VN"));

    private final KnowledgeChunkRepository chunks;
    private final EmbeddingClient embedding;
    private final VectorStore vectorStore;
    private final EventRepository events;
    private final TicketTypeRepository tickets;
    private final TourRepository tours;
    private final TourScheduleRepository schedules;
    private final VenueRepository venues;
    private final LexicalEmbeddingClient fallback = new LexicalEmbeddingClient();

    public IngestService(KnowledgeChunkRepository chunks, EmbeddingClient embedding, VectorStore vectorStore,
                         EventRepository events, TicketTypeRepository tickets, TourRepository tours,
                         TourScheduleRepository schedules, VenueRepository venues) {
        this.chunks = chunks;
        this.embedding = embedding;
        this.vectorStore = vectorStore;
        this.events = events;
        this.tickets = tickets;
        this.tours = tours;
        this.schedules = schedules;
        this.venues = venues;
    }

    @Transactional
    public synchronized int reindexAll() {
        chunks.deleteAll();
        vectorStore.invalidate();
        int total = indexFaq();
        for (Event event : events.findAll()) {
            total += indexEvent(event);
        }
        for (Tour tour : tours.findAll()) {
            total += indexTour(tour);
        }
        vectorStore.invalidate();
        log.info("Reindex xong: {} chunk (provider={})", total, embedding.provider());
        return total;
    }

    @Transactional
    public int indexEvent(Event event) {
        chunks.deleteBySourceTypeAndSourceId("EVENT", event.id);
        StringBuilder sb = new StringBuilder();
        sb.append("Sự kiện: ").append(event.title).append('\n');
        sb.append("Danh mục: ").append(event.category).append('\n');
        sb.append("Thời gian: ").append(DATE_TIME.format(event.startAt)).append(" - ")
                .append(DATE_TIME.format(event.endAt)).append('\n');
        Venue venue = event.venueId == null ? null : venues.findById(event.venueId).orElse(null);
        if (venue != null) {
            sb.append("Địa điểm: ").append(venue.name).append(", ").append(venue.address)
                    .append(", ").append(venue.city).append('\n');
        }
        if (event.description != null) {
            sb.append("\nGiới thiệu:\n").append(event.description).append('\n');
        }
        List<TicketType> types = tickets.findByEventId(event.id);
        if (!types.isEmpty()) {
            sb.append("\nLoại vé đang bán:\n");
            for (TicketType t : types) {
                sb.append("- ").append(t.name).append(": ").append(money(t.price))
                        .append(", còn ").append(t.remaining()).append(" vé")
                        .append(", tối đa ").append(t.maxPerOrder).append(" vé/đơn\n");
            }
        }
        return saveChunks("EVENT", event.id, event.title, sb.toString());
    }

    @Transactional
    public int indexTour(Tour tour) {
        chunks.deleteBySourceTypeAndSourceId("TOUR", tour.id);
        StringBuilder sb = new StringBuilder();
        sb.append("Tour: ").append(tour.title).append('\n');
        sb.append("Điểm đến: ").append(tour.destination)
                .append(" | Khởi hành từ: ").append(tour.departureCity)
                .append(" | Thời lượng: ").append(tour.durationDays).append(" ngày ")
                .append(tour.durationNights).append(" đêm\n");
        sb.append("Giá cơ bản: ").append(money(tour.basePrice)).append('\n');
        if (tour.description != null) {
            sb.append("\nGiới thiệu:\n").append(tour.description).append('\n');
        }
        appendBlock(sb, "Điểm nổi bật", tour.highlights);
        appendBlock(sb, "Bao gồm", tour.included);
        appendBlock(sb, "Không bao gồm", tour.excluded);

        List<TourSchedule> list = schedules.findByTourIdOrderByDepartureDateAsc(tour.id);
        if (!list.isEmpty()) {
            sb.append("\nLịch khởi hành:\n");
            for (TourSchedule s : list) {
                sb.append("- ").append(s.departureDate).append(" đến ").append(s.returnDate)
                        .append(", giá ").append(money(s.price))
                        .append(", còn ").append(s.remaining()).append(" chỗ\n");
            }
        }
        return saveChunks("TOUR", tour.id, tour.title, sb.toString());
    }

    @Transactional
    public int indexFaq() {
        chunks.deleteBySourceType("FAQ");
        int total = 0;
        for (Faq faq : FAQS) {
            total += saveChunks("FAQ", null, faq.title(), faq.title() + "\n\n" + faq.content());
        }
        vectorStore.invalidate();
        return total;
    }

    private void appendBlock(StringBuilder sb, String label, String csv) {
        if (csv == null || csv.isBlank()) {
            return;
        }
        sb.append('\n').append(label).append(":\n");
        for (String line : csv.split("\\r?\\n")) {
            if (!line.isBlank()) {
                sb.append("- ").append(line.trim()).append('\n');
            }
        }
    }

    private int saveChunks(String sourceType, Long sourceId, String title, String text) {
        int count = 0;
        for (Chunker.Chunk chunk : Chunker.split(text)) {
            KnowledgeChunk entity = new KnowledgeChunk();
            entity.sourceType = sourceType;
            entity.sourceId = sourceId;
            entity.title = title;
            entity.chunkIndex = chunk.index();
            entity.content = chunk.content();
            entity.contentHash = sha256(title + "|" + chunk.content());
            float[] vector = safeEmbed(title + "\n" + chunk.content());
            entity.embedding = VectorStore.toJson(vector);
            entity.embeddingModel = embedding.provider();
            entity.embeddingDim = vector.length;
            chunks.save(entity);
            count++;
        }
        return count;
    }

    private float[] safeEmbed(String text) {
        try {
            return embedding.embed(text);
        } catch (RuntimeException ex) {
            log.warn("Embedding that bai ({}), dung lexical tam thoi: {}", embedding.provider(), ex.getMessage());
            return fallback.embed(text);
        }
    }

    private String money(BigDecimal value) {
        return value == null ? "0" : MONEY.format(value) + " VND";
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private record Faq(String title, String content) {
    }

    private static final List<Faq> FAQS = List.of(
            new Faq("Chính sách huỷ vé sự kiện", """
                    Khách được huỷ vé và hoàn 100% nếu huỷ trước giờ diễn ra 7 ngày.
                    Huỷ trong khoảng 3 đến 7 ngày trước giờ diễn ra: hoàn 50%.
                    Huỷ trong vòng 3 ngày trước giờ diễn ra: không hoàn tiền.
                    Vé đã soát tại cổng không được huỷ. Thời gian hoàn tiền từ 5 đến 7 ngày làm việc."""),
            new Faq("Chính sách huỷ tour du lịch", """
                    Huỷ trước 15 ngày khởi hành: hoàn 90% giá tour.
                    Huỷ từ 7 đến 14 ngày: hoàn 50%.
                    Huỷ trong vòng 7 ngày: không hoàn tiền, có thể đổi sang lịch khởi hành khác nếu còn chỗ.
                    Trường hợp bất khả kháng do thiên tai, công ty hoàn 100%."""),
            new Faq("Quy định trẻ em khi tham gia tour", """
                    Trẻ dưới 5 tuổi miễn phí, ngủ chung giường với bố mẹ.
                    Trẻ từ 5 đến 10 tuổi tính 70% giá người lớn.
                    Trẻ từ 11 tuổi trở lên tính như người lớn.
                    Mỗi người lớn chỉ kèm tối đa 2 trẻ em."""),
            new Faq("Hành lý khi đi tour", """
                    Mỗi khách được mang 1 vali 20kg và 1 balo 7kg.
                    Hành lý quá cố định sẽ tính phí theo hãng vận chuyển.
                    Không mang chất lỏng quá 100ml trong hành lý xách tay khi đi máy bay."""),
            new Faq("Cách nhận vé điện tử", """
                    Sau khi thanh toán thành công, vé điện tử có mã QR hiển thị tại mục Vé của tôi.
                    Khách xuất trình mã QR tại cổng soát vé, không cần in giấy.
                    Mỗi mã QR chỉ sử dụng một lần."""),
            new Faq("Phương thức thanh toán", """
                    Hệ thống hỗ trợ thanh toán qua cổng mô phỏng trong đồ án, ví điện tử và chuyển khoản.
                    Đơn hàng được giữ chỗ tối đa 10 phút, quá thời gian này hệ thống tự huỷ và nhả vé."""),
            new Faq("Liên hệ hỗ trợ", """
                    Tổng đài hỗ trợ 1900 0000 từ 8h đến 22h hằng ngày.
                    Email hỗ trợ: hotro@ticketgo.vn.
                    Thời gian phản hồi qua email trong vòng 24 giờ làm việc."""),
            new Faq("Ưu đãi và mã giảm giá", """
                    Mã WELCOME10 giảm 10% tối đa 300.000 đồng cho đơn từ 500.000 đồng.
                    Mã TOUR500 giảm 500.000 đồng cho đơn tour từ 3.000.000 đồng.
                    Mỗi đơn chỉ áp dụng một mã, không cộng dồn."""),
            new Faq("Đổi lịch khởi hành tour", """
                    Được đổi lịch miễn phí trước 7 ngày khởi hành nếu lịch mới còn chỗ.
                    Chênh lệch giá (nếu có) khách thanh toán thêm.
                    Mỗi đơn chỉ được đổi một lần."""),
            new Faq("Quy định hoàn tiền", """
                    Tiền hoàn về đúng phương thức thanh toán ban đầu.
                    Thời gian xử lý 5 đến 7 ngày làm việc kể từ khi yêu cầu được duyệt.
                    Phí chuyển khoản (nếu có) do khách chịu."""));
}
