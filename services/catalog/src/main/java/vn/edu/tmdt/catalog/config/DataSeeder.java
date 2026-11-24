package vn.edu.tmdt.catalog.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.tmdt.catalog.domain.Event;
import vn.edu.tmdt.catalog.domain.ScheduleDef;
import vn.edu.tmdt.catalog.domain.TicketTypeDef;
import vn.edu.tmdt.catalog.domain.Tour;
import vn.edu.tmdt.catalog.domain.Venue;
import vn.edu.tmdt.catalog.repository.EventRepository;
import vn.edu.tmdt.catalog.repository.TourRepository;
import vn.edu.tmdt.catalog.repository.VenueRepository;
import vn.edu.tmdt.catalog.service.CatalogService;
import vn.edu.tmdt.catalog.service.InventoryClient;
import vn.edu.tmdt.catalog.service.SequenceService;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Long ORGANIZER_ID = 2L;

    @Bean
    public ApplicationRunner seedCatalog(EventRepository events, TourRepository tours,
                                         VenueRepository venues, SequenceService sequence,
                                         InventoryClient inventory) {
        return args -> {
            if (events.count() > 0) {
                return;
            }
            Venue haNoi = venue(venues, sequence, "Nhà hát lớn Hà Nội", "1 Tràng Tiền, Hoàn Kiếm", "Hà Nội", 600);
            Venue hcm = venue(venues, sequence, "Nhà hát Bến Thành", "6 Mạc Đĩnh Chi, Quận 1", "TP. Hồ Chí Minh", 500);
            Venue outdoor = venue(venues, sequence, "Công viên Ánh Sáng", "Khu đô thị Phú Mỹ Hưng, Quận 7", "TP. Hồ Chí Minh", 1500);

            Event concert = event(events, sequence, "Đêm nhạc Trịnh Công Sơn — Tình yêu và biển",
                    "music", haNoi, at(2026, 12, 20, 20, 0), at(2026, 12, 20, 22, 0),
                    "Đêm nhạc tưởng nhớ Trịnh Công Sơn với dàn nhạc giao hưởng, hai phần Tình yêu và Biển. "
                            + "Thời lượng 120 phút, phù hợp khán giả từ 12 tuổi.",
                    "https://picsum.photos/seed/trinh/1200/700");
            ticket(concert, sequence, "VIP", 1500000, 60, 4);
            ticket(concert, sequence, "Standard", 700000, 200, 6);

            Event comedy = event(events, sequence, "Hài kịch: Chuyện Nhà Bên",
                    "theater", hcm, at(2026, 12, 5, 19, 30), at(2026, 12, 5, 21, 15),
                    "Vở hài kịch tình huống về khu chung cư cũ, dài 105 phút, phù hợp khán giả từ 10 tuổi.",
                    "https://picsum.photos/seed/comedy/1200/700");
            ticket(comedy, sequence, "Standard", 350000, 300, 6);
            ticket(comedy, sequence, "Couple", 600000, 80, 2);

            Event workshop = event(events, sequence, "Workshop: Nhiếp ảnh đường phố cùng máy ảnh điện thoại",
                    "workshop", hcm, at(2026, 12, 12, 9, 0), at(2026, 12, 12, 12, 0),
                    "Workshop 3 giờ về bố cục, ánh sáng và đạo đức khi chụp ảnh đường phố. Giới hạn 30 học viên.",
                    "https://picsum.photos/seed/photo/1200/700");
            ticket(workshop, sequence, "Học viên", 450000, 30, 2);

            Event run = event(events, sequence, "Giải chạy đêm Sài Gòn 2026 — 5km và 10km",
                    "sport", outdoor, at(2027, 1, 10, 18, 0), at(2027, 1, 10, 22, 0),
                    "Giải chạy đêm ven sông với cự ly 5km và 10km, có nước uống, y tế và huy chương.",
                    "https://picsum.photos/seed/run/1200/700");
            ticket(run, sequence, "5km", 250000, 500, 4);
            ticket(run, sequence, "10km", 350000, 500, 4);

            concert.slug = "dem-nhac-trinh-cong-son";
            comedy.slug = "hai-kich-chuyen-nha-ben";
            workshop.slug = "workshop-nhiep-anh-duong-pho";
            run.slug = "giai-chay-dem-sai-gon-2026";

            Tour dalat3 = tour(tours, sequence, "Tour Đà Lạt 3N2Đ — Săn mây và cà phê giữa núi",
                    "Đà Lạt", "TP. Hồ Chí Minh", 3, 2, 2890000,
                    "Tour 3 ngày 2 đêm: Đồi chè Cầu Đất, hồ Tuyền Lâm, chùa Linh Phước, chợ đêm Đà Lạt. "
                            + "Khách sạn 3 sao, xe giường nằm khứ hồi, có ăn sáng.",
                    "https://picsum.photos/seed/dalat3/1200/700");
            schedule(dalat3, sequence, LocalDate.of(2026, 12, 5), 2890000, 24);
            schedule(dalat3, sequence, LocalDate.of(2026, 12, 19), 2990000, 20);

            Tour dalat4 = tour(tours, sequence, "Tour Đà Lạt 4N3Đ — Thác Datanla và làng Cù Lần",
                    "Đà Lạt", "TP. Hồ Chí Minh", 4, 3, 3990000,
                    "Tour 4 ngày 3 đêm có thêm thác Datanla, làng Cù Lần, chèo thuyền hồ Tuyền Lâm. "
                            + "Lịch trình nhẹ nhàng, phù hợp gia đình.",
                    "https://picsum.photos/seed/dalat4/1200/700");
            schedule(dalat4, sequence, LocalDate.of(2026, 12, 12), 3990000, 18);
            schedule(dalat4, sequence, LocalDate.of(2027, 1, 9), 4190000, 22);

            Tour phuQuoc = tour(tours, sequence, "Tour Phú Quốc 4N3Đ — Lặn ngắm san hô",
                    "Phú Quốc", "TP. Hồ Chí Minh", 4, 3, 5490000,
                    "Tour 4 ngày 3 đêm: lặn san hô Hòn Thơm, cáp treo vượt biển, làng chài Hàm Ninh. "
                            + "Không bao gồm vé máy bay; resort 3 sao sát biển.",
                    "https://picsum.photos/seed/phuquoc/1200/700");
            schedule(phuQuoc, sequence, LocalDate.of(2026, 12, 18), 5490000, 16);
            schedule(phuQuoc, sequence, LocalDate.of(2027, 1, 15), 5790000, 20);

            Tour haGiang = tour(tours, sequence, "Tour Hà Giang 3N2Đ — Cung đường đá và hoa tam giác mạch",
                    "Hà Giang", "Hà Nội", 3, 2, 2490000,
                    "Tour 3 ngày 2 đêm qua Đồng Văn, Mèo Vạc, đèo Mã Pí Lèng và sông Nho Quế. "
                            + "Nhiều đường đèo, không phù hợp trẻ dưới 5 tuổi.",
                    "https://picsum.photos/seed/hagiang/1200/700");
            schedule(haGiang, sequence, LocalDate.of(2026, 12, 3), 2490000, 14);
            schedule(haGiang, sequence, LocalDate.of(2027, 1, 8), 2590000, 14);

            Tour hoiAn = tour(tours, sequence, "Tour Hội An 2N1Đ — Phố cổ và rừng dừa Bảy Mẫu",
                    "Hội An", "Đà Nẵng", 2, 1, 1690000,
                    "Tour 2 ngày 1 đêm: phố cổ, thả đèn hoa đăng, rừng dừa Bảy Mẫu. "
                            + "Khởi hành từ Đà Nẵng, không gồm vé máy bay.",
                    "https://picsum.photos/seed/hoian/1200/700");
            schedule(hoiAn, sequence, LocalDate.of(2026, 12, 6), 1690000, 20);
            schedule(hoiAn, sequence, LocalDate.of(2026, 12, 27), 1790000, 20);

            dalat3.slug = "tour-da-lat-3n2d";
            dalat4.slug = "tour-da-lat-4n3d";
            phuQuoc.slug = "tour-phu-quoc-4n3d";
            haGiang.slug = "tour-ha-giang-3n2d";
            hoiAn.slug = "tour-hoi-an-2n1d";

            events.saveAll(List.of(concert, comedy, workshop, run));
            tours.saveAll(List.of(dalat3, dalat4, phuQuoc, haGiang, hoiAn));

            ensure(events, tours, inventory);
            log.info("Da seed {} su kien, {} tour", events.count(), tours.count());
        };
    }

    private Venue venue(VenueRepository venues, SequenceService sequence, String name, String address,
                        String city, int capacity) {
        Venue venue = new Venue();
        venue.id = sequence.next("venue");
        venue.name = name;
        venue.address = address;
        venue.city = city;
        venue.capacity = capacity;
        return venues.save(venue);
    }

    private Event event(EventRepository events, SequenceService sequence, String title, String category,
                        Venue venue, Instant startAt, Instant endAt, String description, String image) {
        Event event = new Event();
        event.id = sequence.next("event");
        event.organizerId = ORGANIZER_ID;
        event.title = title;
        event.slug = CatalogService.slugify(title);
        event.category = category;
        event.description = description;
        event.coverImageUrl = image;
        event.venueName = venue.name;
        event.address = venue.address;
        event.city = venue.city;
        event.startAt = startAt;
        event.endAt = endAt;
        event.createdAt = Instant.now();
        event.status = "PUBLISHED";
        return event;
    }

    private void ticket(Event event, SequenceService sequence, String name, long price, int quota,
                        int maxPerOrder) {
        TicketTypeDef ticket = new TicketTypeDef();
        ticket.id = sequence.next("ticket");
        ticket.name = name;
        ticket.price = BigDecimal.valueOf(price);
        ticket.quota = quota;
        ticket.maxPerOrder = maxPerOrder;
        event.ticketTypes.add(ticket);
    }

    private Tour tour(TourRepository tours, SequenceService sequence, String title, String destination,
                      String departureCity, int days, int nights, long price, String description,
                      String image) {
        Tour tour = new Tour();
        tour.id = sequence.next("tour");
        tour.organizerId = ORGANIZER_ID;
        tour.title = title;
        tour.slug = CatalogService.slugify(title);
        tour.destination = destination;
        tour.departureCity = departureCity;
        tour.durationDays = days;
        tour.durationNights = nights;
        tour.basePrice = BigDecimal.valueOf(price);
        tour.description = description;
        tour.coverImageUrl = image;
        tour.createdAt = Instant.now();
        tour.status = "PUBLISHED";
        tour.highlights = List.of("Khám phá điểm nổi bật", "Ẩm thực địa phương", "Hướng dẫn viên suốt tuyến");
        tour.included = List.of("Khách sạn", "Ăn sáng", "Xe đưa đón", "Vé tham quan");
        tour.excluded = List.of("Chi phí cá nhân", "Đồ uống ngoài chương trình");
        return tour;
    }

    private void schedule(Tour tour, SequenceService sequence, LocalDate departure, long price, int seats) {
        ScheduleDef schedule = new ScheduleDef();
        schedule.id = sequence.next("schedule");
        schedule.departureDate = departure;
        schedule.returnDate = departure.plusDays(Math.max(1, tour.durationDays - 1));
        schedule.price = BigDecimal.valueOf(price);
        schedule.seatsTotal = seats;
        tour.schedules.add(schedule);
    }

    private void ensure(EventRepository events, TourRepository tours, InventoryClient inventory) {
        events.findAll().forEach(event -> inventory.ensure(event.ticketTypes.stream()
                .map(ticket -> new InventoryClient.StockDef("TICKET", ticket.id,
                        event.title + " - " + ticket.name, ticket.price, ticket.quota,
                        ticket.maxPerOrder, event.organizerId))
                .toList()));
        tours.findAll().forEach(tour -> inventory.ensure(tour.schedules.stream()
                .map(schedule -> new InventoryClient.StockDef("SCHEDULE", schedule.id,
                        tour.title + " - " + schedule.departureDate, schedule.price,
                        schedule.seatsTotal, 10, tour.organizerId))
                .toList()));
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).atZone(ZONE).toInstant();
    }
}
