package vn.edu.tmdt.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import vn.edu.tmdt.ai.IngestService;
import vn.edu.tmdt.ai.KnowledgeChunkRepository;
import vn.edu.tmdt.auth.Role;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.auth.UserRepository;
import vn.edu.tmdt.booking.Voucher;
import vn.edu.tmdt.booking.VoucherRepository;
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
import vn.edu.tmdt.common.Slugs;

/**
 * Du lieu demo: 3 dia diem, 4 su kien, 5 tour, voucher, va index RAG lan dau.
 * Chi chay khi database con trong.
 */
@Configuration
@Profile("!test")
public class DemoCatalogSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoCatalogSeeder.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    @Order(2)
    public ApplicationRunner seedCatalog(VenueRepository venues, EventRepository events,
                                         TicketTypeRepository tickets, TourRepository tours,
                                         TourScheduleRepository schedules, VoucherRepository vouchers,
                                         UserRepository users, IngestService ingest,
                                         KnowledgeChunkRepository chunks) {
        return args -> {
            if (events.count() == 0) {
                User organizer = users.findByEmailIgnoreCase("organizer@demo.vn")
                        .orElseThrow(() -> new IllegalStateException("Thieu tai khoan organizer"));

                Venue haNoi = venue(venues, "Nhà hát lớn Hà Nội", "1 Tràng Tiền, Hoàn Kiếm", "Hà Nội", 600);
                Venue hcm = venue(venues, "Nhà hát Bến Thành", "6 Mạc Đĩnh Chi, Quận 1", "TP. Hồ Chí Minh", 500);
                Venue outdoor = venue(venues, "Công viên Ánh Sáng", "Khu đô thị Phú Mỹ Hưng, Quận 7", "TP. Hồ Chí Minh", 1500);

                Event concert = event(events, organizer.getId(), haNoi.id,
                        "Đêm nhạc Trịnh Công Sơn — Tình yêu và biển", "music", 2026, 12, 20, 20, 0, 2,
                        "https://picsum.photos/seed/trinh/1200/700",
                        """
                        Đêm nhạc tưởng nhớ Trịnh Công Sơn với sự tham gia của dàn nhạc giao hưởng và ba giọng ca khách mời.
                        Chương trình gồm hai phần: "Tình yêu" với những bản tình ca quen thuộc và "Biển" với các ca khúc
                        viết về biển đảo, quê hương. Sân khấu được thiết kế theo phong cách tối giản, ánh sáng ấm.

                        Thời lượng khoảng 120 phút, có nghỉ giữa hiệp 15 phút. Khán giả được mang theo máy ảnh không flash.
                        Chương trình phù hợp với khán giả từ 12 tuổi trở lên.""");
                ticket(tickets, concert.id, "VIP", new BigDecimal("1500000"), 60, 4,
                        "Ghế hàng đầu, có lối đi riêng và đồ uống chào mừng");
                ticket(tickets, concert.id, "Standard", new BigDecimal("700000"), 200, 6,
                        "Ghế khu vực giữa và sau, tầm nhìn đầy đủ sân khấu");

                Event comedy = event(events, organizer.getId(), hcm.id,
                        "Hài kịch: Chuyện Nhà Bên", "theater", 2026, 12, 5, 19, 30, 2,
                        "https://picsum.photos/seed/comedy/1200/700",
                        """
                        Vở hài kịch tình huống về một khu chung cư cũ, nơi hàng xóm can thiệp vào đời sống của nhau
                        bằng những "lời khuyên" không ai yêu cầu. Kịch bản dựa trên các tình huống đời thường, nhiều
                        tình tiết bất ngờ, phù hợp để thư giãn cuối tuần.

                        Vở diễn dài 105 phút, không có nghỉ giữa hiệp. Khán giả từ 10 tuổi trở lên.""");
                ticket(tickets, comedy.id, "Standard", new BigDecimal("350000"), 300, 6, null);
                ticket(tickets, comedy.id, "Couple", new BigDecimal("600000"), 80, 2,
                        "Gói hai ghế cạnh nhau kèm nước uống");

                Event workshop = event(events, organizer.getId(), hcm.id,
                        "Workshop: Nhiếp ảnh đường phố cùng máy ảnh điện thoại", "workshop", 2026, 12, 12, 9, 0, 3,
                        "https://picsum.photos/seed/photo/1200/700",
                        """
                        Buổi workshop kéo dài 3 giờ, hướng dẫn chụp ảnh đường phố bằng điện thoại: bố cục, ánh sáng tự nhiên,
                        cách tiếp cận nhân vật và đạo đức khi chụp ảnh người lạ. Học viên thực hành theo nhóm tại khu vực
                        chợ Bến Thành và phố đi bộ Nguyễn Huệ.

                        Học viên cần mang theo điện thoại còn pin và giày thoải mái. Số lượng giới hạn 30 người để đảm bảo
                        chất lượng hướng dẫn.""");
                ticket(tickets, workshop.id, "Học viên", new BigDecimal("450000"), 30, 2, null);

                Event run = event(events, organizer.getId(), outdoor.id,
                        "Giải chạy đêm Sài Gòn 2026 — 5km và 10km", "sport", 2027, 1, 10, 18, 0, 4,
                        "https://picsum.photos/seed/run/1200/700",
                        """
                        Giải chạy đêm với hai cự ly 5km và 10km, đường chạy ven sông qua các cây cầu nổi tiếng của thành phố.
                        Ban tổ chức bố trí nước uống, y tế và ánh sáng dọc đường chạy. Người hoàn thành nhận huy chương
                        và áo thi đấu.

                        Thời gian xuất phát lúc 18h, đóng cổng xuất phát sau 15 phút. Vận động viên cần có giấy xác nhận
                        sức khoẻ nếu trên 60 tuổi.""");
                ticket(tickets, run.id, "5km", new BigDecimal("250000"), 500, 4, null);
                ticket(tickets, run.id, "10km", new BigDecimal("350000"), 500, 4, null);

                Tour dalat3 = tour(tours, organizer.getId(), "Tour Đà Lạt 3N2Đ — Săn mây và cà phê giữa núi",
                        "Đà Lạt", "TP. Hồ Chí Minh", 3, 2, new BigDecimal("2890000"),
                        "https://picsum.photos/seed/dalat3/1200/700",
                        """
                        Tour Đà Lạt 3 ngày 2 đêm tập trung vào trung tâm thành phố và vùng ven: Đồi chè Cầu Đất, hồ Tuyền Lâm,
                        chùa Linh Phước, chợ đêm Đà Lạt. Đoàn di chuyển bằng xe giường nằm khởi hành buổi tối từ TP. Hồ Chí Minh,
                        đến Đà Lạt vào sáng sớm ngày thứ hai để kịp săn mây.

                        Khách nghỉ tại khách sạn 3 sao khu vực trung tâm, có ăn sáng. Lịch trình có một buổi tự do để khách
                        tự khám phá quán cà phê hoặc mua đặc sản. Tour phù hợp với nhóm bạn trẻ và gia đình có trẻ trên 6 tuổi.

                        Điểm nhấn của tour là buổi sáng săn mây tại Đồi chè Cầu Đất và trải nghiệm uống cà phê muối tại quán
                        view thung lũng. Ngày thứ ba đoàn ghé chợ đêm mua đặc sản trước khi về lại thành phố.""",
                        """
                        Săn mây tại Đồi chè Cầu Đất lúc bình minh
                        Check-in quán cà phê view thung lũng
                        Tham quan hồ Tuyền Lâm và chùa Linh Phước
                        Ăn tối tại chợ đêm Đà Lạt
                        """,
                        """
                        Xe giường nằm khứ hồi TP. Hồ Chí Minh - Đà Lạt
                        Khách sạn 3 sao, 2 đêm, phòng đôi
                        Ăn sáng 2 bữa, ăn trưa 2 bữa
                        Vé tham quan theo lịch trình
                        Hướng dẫn viên suốt tuyến
                        """,
                        """
                        Chi phí cá nhân, đồ uống ngoài chương trình
                        Phụ thu phòng đơn 600.000 đồng/đêm
                        Bảo hiểm du lịch tự nguyện
                        """);
                schedule(schedules, dalat3.id, LocalDate.of(2026, 12, 5), 2890000, 24);
                schedule(schedules, dalat3.id, LocalDate.of(2026, 12, 19), 2990000, 20);

                Tour dalat4 = tour(tours, organizer.getId(), "Tour Đà Lạt 4N3Đ — Thác Datanla và làng Cù Lần",
                        "Đà Lạt", "TP. Hồ Chí Minh", 4, 3, new BigDecimal("3990000"),
                        "https://picsum.photos/seed/dalat4/1200/700",
                        """
                        Tour Đà Lạt 4 ngày 3 đêm có thêm một ngày so với tour 3 ngày 2 đêm, dành cho khách muốn trải nghiệm
                        sâu hơn vùng ven: thác Datanla, làng Cù Lần, hồ Tuyền Lâm và đồi chè. Ngày thứ ba đoàn dành trọn
                        một ngày ở khu vực ngoại ô, tối về trung tâm tự do.

                        Khách nghỉ tại khách sạn 3 sao, có ăn sáng và 3 bữa trưa. Tour phù hợp với gia đình có trẻ nhỏ và
                        người lớn tuổi vì lịch trình nhẹ nhàng hơn, có nhiều thời gian nghỉ.

                        So với tour 3N2Đ, tour 4N3Đ có thêm hoạt động đi bộ trong rừng thông, chèo thuyền tại hồ Tuyền Lâm
                        và một buổi tối giao lưu cồng chiêng. Giá tour cao hơn khoảng 1.100.000 đồng do thêm một đêm khách sạn
                        và một ngày xe.""",
                        """
                        Thác Datanla và máng trượt
                        Làng Cù Lần
                        Chèo thuyền hồ Tuyền Lâm
                        Đồi chè Cầu Đất
                        """,
                        """
                        Xe giường nằm khứ hồi
                        Khách sạn 3 sao, 3 đêm
                        Ăn sáng 3 bữa, ăn trưa 3 bữa
                        Vé tham quan và hoạt động theo lịch trình
                        Hướng dẫn viên
                        """,
                        """
                        Chi phí cá nhân
                        Phụ thu phòng đơn 600.000 đồng/đêm
                        Đồ uống ngoài chương trình
                        """);
                schedule(schedules, dalat4.id, LocalDate.of(2026, 12, 12), 3990000, 18);
                schedule(schedules, dalat4.id, LocalDate.of(2027, 1, 9), 4190000, 22);

                Tour phuQuoc = tour(tours, organizer.getId(), "Tour Phú Quốc 4N3Đ — Lặn ngắm san hô",
                        "Phú Quốc", "TP. Hồ Chí Minh", 4, 3, new BigDecimal("5490000"),
                        "https://picsum.photos/seed/phuquoc/1200/700",
                        """
                        Tour Phú Quốc 4 ngày 3 đêm kết hợp nghỉ dưỡng và khám phá biển: lặn ngắm san hô tại Hòn Thơm,
                        cáp treo vượt biển dài nhất Việt Nam, làng chài Hàm Ninh và chợ đêm Dinh Cậu. Khách nghỉ tại
                        resort 3 sao sát biển, có bữa sáng buffet.

                        Tour không bao gồm vé máy bay, khách tự túc di chuyển đến Phú Quốc hoặc đăng ký thêm gói vé máy bay
                        của công ty. Thời gian tự do buổi chiều ngày thứ ba để khách tắm biển hoặc nghỉ tại resort.

                        Đây là tour phù hợp với khách thích biển đảo, gia đình và cặp đôi. Hoạt động lặn ngắm san hô có
                        hướng dẫn viên riêng và áo phao, không yêu cầu biết bơi.""",
                        """
                        Lặn ngắm san hô tại Hòn Thơm
                        Cáp treo vượt biển
                        Làng chài Hàm Ninh
                        Chợ đêm Dinh Cậu
                        """,
                        """
                        Khách sạn 3 sao sát biển, 3 đêm, ăn sáng buffet
                        Xe đưa đón sân bay trong đảo
                        Vé cáp treo và tour lặn
                        Hướng dẫn viên
                        """,
                        """
                        Vé máy bay khứ hồi
                        Bữa trưa và bữa tối
                        Chi phí cá nhân
                        """);
                schedule(schedules, phuQuoc.id, LocalDate.of(2026, 12, 18), 5490000, 16);
                schedule(schedules, phuQuoc.id, LocalDate.of(2027, 1, 15), 5790000, 20);

                Tour haGiang = tour(tours, organizer.getId(), "Tour Hà Giang 3N2Đ — Cung đường đá và hoa tam giác mạch",
                        "Hà Giang", "Hà Nội", 3, 2, new BigDecimal("2490000"),
                        "https://picsum.photos/seed/hagiang/1200/700",
                        """
                        Tour Hà Giang 3 ngày 2 đêm theo cung đường Đồng Văn - Mèo Vạc, qua đèo Mã Pí Lèng, sông Nho Quế
                        và cột cờ Lũng Cú. Đoàn di chuyển bằng xe 16 chỗ, nghỉ tại homestay và khách sạn nhỏ tại Đồng Văn.

                        Lịch trình có nhiều đoạn đường đèo, phù hợp với khách thích khám phá và chụp ảnh. Khách cần chuẩn bị
                        áo ấm vì nhiệt độ ban đêm có thể xuống dưới 10 độ vào mùa đông. Tour không phù hợp với trẻ dưới 5 tuổi.

                        Điểm nhấn là chèo thuyền trên sông Nho Quế và ngắm hoa tam giác mạch vào tháng 11, 12.""",
                        """
                        Đèo Mã Pí Lèng
                        Chèo thuyền sông Nho Quế
                        Cột cờ Lũng Cú
                        Phố cổ Đồng Văn
                        """,
                        """
                        Xe 16 chỗ khứ hồi từ Hà Nội
                        Homestay 2 đêm
                        Ăn sáng 2 bữa, ăn tối 2 bữa
                        Thuyền sông Nho Quế
                        Hướng dẫn viên
                        """,
                        """
                        Chi phí cá nhân
                        Bữa trưa
                        Phụ thu phòng đơn
                        """);
                schedule(schedules, haGiang.id, LocalDate.of(2026, 12, 3), 2490000, 14);
                schedule(schedules, haGiang.id, LocalDate.of(2027, 1, 8), 2590000, 14);

                Tour hoiAn = tour(tours, organizer.getId(), "Tour Hội An 2N1Đ — Phố cổ và rừng dừa Bảy Mẫu",
                        "Hội An", "Đà Nẵng", 2, 1, new BigDecimal("1690000"),
                        "https://picsum.photos/seed/hoian/1200/700",
                        """
                        Tour Hội An 2 ngày 1 đêm dành cho khách muốn nghỉ ngắn cuối tuần: dạo phố cổ, thả đèn hoa đăng
                        trên sông Hoài, đi thuyền thúng tại rừng dừa Bảy Mẫu. Khách nghỉ tại khách sạn 3 sao cách phố cổ
                        500 mét, có ăn sáng.

                        Tour khởi hành từ Đà Nẵng, phù hợp với khách đã có vé máy bay. Thời gian tự do buổi chiều ngày
                        thứ hai để mua sắm hoặc nghỉ ngơi trước khi về.

                        Điểm nhấn là buổi tối thả đèn hoa đăng và ẩm thực đường phố Hội An: cao lầu, cơm gà, bánh mì Phượng.""",
                        """
                        Phố cổ Hội An về đêm
                        Thả đèn hoa đăng sông Hoài
                        Rừng dừa Bảy Mẫu
                        Ẩm thực đường phố
                        """,
                        """
                        Khách sạn 3 sao 1 đêm, ăn sáng
                        Xe đưa đón từ Đà Nẵng
                        Vé tham quan phố cổ
                        Thuyền thúng rừng dừa
                        Hướng dẫn viên
                        """,
                        """
                        Vé máy bay
                        Bữa trưa, bữa tối
                        Chi phí cá nhân
                        """);
                schedule(schedules, hoiAn.id, LocalDate.of(2026, 12, 6), 1690000, 20);
                schedule(schedules, hoiAn.id, LocalDate.of(2026, 12, 27), 1790000, 20);

                voucher(vouchers, "WELCOME10", "Chào mừng khách mới", "PERCENT", 10, 500000,
                        300000L, 200, 2026, 11, 1, 2027, 6, 30);
                voucher(vouchers, "TOUR500", "Giảm 500k cho tour", "FIXED", 500000, 3000000,
                        null, 100, 2026, 11, 1, 2027, 3, 31);
                voucher(vouchers, "HETHONG", "Mã đã hết hạn (dùng để demo lỗi)", "PERCENT", 20, 0,
                        null, 50, 2025, 1, 1, 2025, 12, 31);

                log.info("Da seed {} su kien, {} tour, {} voucher", events.count(), tours.count(), vouchers.count());
            }

            if (chunks.count() == 0) {
                ingest.reindexAll();
            }
        };
    }

    private Venue venue(VenueRepository repo, String name, String address, String city, int capacity) {
        Venue venue = new Venue();
        venue.name = name;
        venue.address = address;
        venue.city = city;
        venue.capacity = capacity;
        return repo.save(venue);
    }

    private Event event(EventRepository repo, Long organizerId, Long venueId, String title, String category,
                        int year, int month, int day, int hour, int minute, int durationHours,
                        String cover, String description) {
        Event event = new Event();
        event.organizerId = organizerId;
        event.venueId = venueId;
        event.title = title;
        event.slug = Slugs.of(title);
        event.category = category;
        event.coverImageUrl = cover;
        event.description = description.indent(0).strip();
        event.startAt = LocalDateTime.of(year, month, day, hour, minute).atZone(ZONE).toInstant();
        event.endAt = event.startAt.plusSeconds(durationHours * 3600L);
        event.saleStartAt = LocalDateTime.now().minusDays(1).atZone(ZONE).toInstant();
        event.saleEndAt = event.startAt;
        event.status = "PUBLISHED";
        return repo.save(event);
    }

    private void ticket(TicketTypeRepository repo, Long eventId, String name, BigDecimal price,
                        int quota, int maxPerOrder, String description) {
        TicketType ticket = new TicketType();
        ticket.eventId = eventId;
        ticket.name = name;
        ticket.price = price;
        ticket.quota = quota;
        ticket.soldCount = 0;
        ticket.maxPerOrder = maxPerOrder;
        ticket.description = description;
        repo.save(ticket);
    }

    private Tour tour(TourRepository repo, Long organizerId, String title, String destination,
                      String departure, int days, int nights, BigDecimal price, String cover,
                      String description, String highlights, String included, String excluded) {
        Tour tour = new Tour();
        tour.organizerId = organizerId;
        tour.title = title;
        tour.slug = Slugs.of(title);
        tour.destination = destination;
        tour.departureCity = departure;
        tour.durationDays = days;
        tour.durationNights = nights;
        tour.basePrice = price;
        tour.coverImageUrl = cover;
        tour.description = description.indent(0).strip();
        tour.highlights = highlights.indent(0).strip();
        tour.included = included.indent(0).strip();
        tour.excluded = excluded.indent(0).strip();
        tour.status = "PUBLISHED";
        return repo.save(tour);
    }

    private void schedule(TourScheduleRepository repo, Long tourId, LocalDate departure,
                          long price, int seats) {
        TourSchedule schedule = new TourSchedule();
        schedule.tourId = tourId;
        schedule.departureDate = departure;
        schedule.returnDate = departure.plusDays(2);
        schedule.price = BigDecimal.valueOf(price);
        schedule.seatsTotal = seats;
        schedule.seatsBooked = 0;
        schedule.status = "OPEN";
        repo.save(schedule);
    }

    private void voucher(VoucherRepository repo, String code, String name, String type, long value,
                         long minOrder, Long maxDiscount, int limit,
                         int fromYear, int fromMonth, int fromDay,
                         int toYear, int toMonth, int toDay) {
        Voucher voucher = new Voucher();
        voucher.code = code;
        voucher.name = name;
        voucher.discountType = type;
        voucher.discountValue = BigDecimal.valueOf(value);
        voucher.minOrderAmount = BigDecimal.valueOf(minOrder);
        voucher.maxDiscountAmount = maxDiscount == null ? null : BigDecimal.valueOf(maxDiscount);
        voucher.usageLimit = limit;
        voucher.usedCount = 0;
        voucher.validFrom = LocalDate.of(fromYear, fromMonth, fromDay).atStartOfDay(ZONE).toInstant();
        voucher.validTo = LocalDate.of(toYear, toMonth, toDay).atTime(23, 59).atZone(ZONE).toInstant();
        voucher.active = true;
        repo.save(voucher);
    }
}
