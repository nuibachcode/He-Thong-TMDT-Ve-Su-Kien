package vn.edu.tmdt.inventory.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.inventory.common.AppException;
import vn.edu.tmdt.inventory.domain.Hold;
import vn.edu.tmdt.inventory.domain.Stock;
import vn.edu.tmdt.inventory.domain.Ticket;
import vn.edu.tmdt.inventory.repository.HoldRepository;
import vn.edu.tmdt.inventory.repository.StockRepository;
import vn.edu.tmdt.inventory.repository.TicketRepository;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    public record StockDef(String refType, Long refId, String name, BigDecimal price, int total,
                           int maxPerOrder, Long organizerId) {
    }

    public record StockView(String refType, Long refId, String name, BigDecimal price, int total,
                            int sold, int remaining, Integer maxPerOrder, Long organizerId) {
    }

    public record StockQuery(String refType, List<Long> ids) {
    }

    public record HoldItem(String refType, Long refId, int quantity) {
    }

    public record HoldRequest(String bookingCode, Instant expiresAt, List<HoldItem> items) {
    }

    public record HoldLine(String refType, Long refId, String itemName, int quantity,
                           BigDecimal unitPrice, BigDecimal amount, Long organizerId) {
    }

    public record HoldResult(String bookingCode, List<HoldLine> items, BigDecimal total) {
    }

    public record TicketView(Long ticketId, String itemName, int quantity, String qrPngBase64,
                             Instant checkedInAt) {
    }

    public record OrganizerStat(long sold, long quota) {
    }

    private final StockRepository stocks;
    private final HoldRepository holds;
    private final TicketRepository tickets;
    private final QrService qr;
    private final StringRedisTemplate redis;

    public InventoryService(StockRepository stocks, HoldRepository holds, TicketRepository tickets,
                            QrService qr, StringRedisTemplate redis) {
        this.stocks = stocks;
        this.holds = holds;
        this.tickets = tickets;
        this.qr = qr;
        this.redis = redis;
    }

    @Transactional
    public List<StockView> ensure(List<StockDef> defs) {
        List<StockView> result = new ArrayList<>();
        for (StockDef def : defs) {
            Stock stock = stocks.findByRefTypeAndRefId(def.refType(), def.refId()).orElseGet(Stock::new);
            stock.refType = def.refType();
            stock.refId = def.refId();
            stock.name = def.name();
            stock.price = def.price();
            stock.maxPerOrder = def.maxPerOrder() <= 0 ? 4 : def.maxPerOrder();
            stock.organizerId = def.organizerId();
            int total = Math.max(0, def.total());
            stock.total = Math.max(total, stock.sold);
            stocks.save(stock);
            result.add(view(stock));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StockView> query(String refType, List<Long> ids) {
        return stocks.findByRefTypeAndRefIdIn(refType, ids).stream().map(this::view).toList();
    }

    @Transactional
    public HoldResult hold(HoldRequest request) {
        if (request.bookingCode() == null || request.bookingCode().isBlank()) {
            throw AppException.badRequest("Thieu ma don");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw AppException.badRequest("Don phai co it nhat 1 ve");
        }
        Instant expiresAt = request.expiresAt() == null ? Instant.now().plusSeconds(600) : request.expiresAt();
        List<Hold> rows = new ArrayList<>();
        List<HoldLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (HoldItem item : request.items()) {
            int qty = item.quantity();
            if (qty < 1) {
                throw AppException.badRequest("So luong phai lon hon 0");
            }
            Stock stock = stocks.findByRefTypeAndRefId(item.refType(), item.refId())
                    .orElseThrow(() -> AppException.conflict("Chua khoi tao ton kho cho " + item.refId()));
            if (qty > stock.maxPerOrder) {
                throw AppException.conflict("Moi don chi duoc mua toi da " + stock.maxPerOrder + " " + stock.name);
            }
            if (stocks.reserve(item.refType(), item.refId(), qty) == 0) {
                throw AppException.conflict(stock.name + " chi con " + stock.remaining() + " ve");
            }
            Hold hold = new Hold();
            hold.bookingCode = request.bookingCode();
            hold.refType = item.refType();
            hold.refId = item.refId();
            hold.itemName = stock.name;
            hold.quantity = qty;
            hold.unitPrice = stock.price;
            hold.organizerId = stock.organizerId == null ? 0L : stock.organizerId;
            hold.expiresAt = expiresAt;
            hold.status = "PENDING";
            rows.add(hold);
            BigDecimal amount = stock.price.multiply(BigDecimal.valueOf(qty));
            lines.add(new HoldLine(item.refType(), item.refId(), stock.name, qty, stock.price, amount,
                    hold.organizerId));
            total = total.add(amount);
        }
        holds.saveAll(rows);
        redisTtl(request.bookingCode(), expiresAt);
        return new HoldResult(request.bookingCode(), lines, total);
    }

    @Transactional
    public String confirm(String bookingCode) {
        List<Hold> rows = holds.findByBookingCodeAndStatus(bookingCode, "PENDING");
        if (rows.isEmpty()) {
            if (!tickets.findByBookingCodeOrderByIdAsc(bookingCode).isEmpty()) {
                return "OK";
            }
            throw AppException.conflict("Don da het han giu cho");
        }
        for (Hold hold : rows) {
            hold.status = "CONFIRMED";
            Ticket ticket = new Ticket();
            ticket.bookingCode = bookingCode;
            ticket.itemName = hold.itemName;
            ticket.quantity = hold.quantity;
            ticket.organizerId = hold.organizerId;
            ticket.qrPayload = "TICKET|" + bookingCode + "|" + hold.id + "|" + hold.itemName;
            tickets.save(ticket);
        }
        holds.saveAll(rows);
        redis.delete(redisKey(bookingCode));
        return "OK";
    }

    @Transactional
    public void release(String bookingCode) {
        List<Hold> rows = holds.findByBookingCodeOrderByIdAsc(bookingCode);
        for (Hold hold : rows) {
            if (!"RELEASED".equals(hold.status)) {
                stocks.release(hold.refType, hold.refId, hold.quantity);
                hold.status = "RELEASED";
            }
        }
        holds.saveAll(rows);
        tickets.deleteByBookingCode(bookingCode);
        redis.delete(redisKey(bookingCode));
    }

    @Transactional(readOnly = true)
    public List<TicketView> tickets(String bookingCode) {
        return tickets.findByBookingCodeOrderByIdAsc(bookingCode).stream()
                .map(ticket -> new TicketView(ticket.id, ticket.itemName, ticket.quantity,
                        qr.pngBase64(ticket.qrPayload, 260), ticket.checkedInAt))
                .toList();
    }

    @Transactional
    public String checkin(String bookingCode, Long ticketId, Long organizerId) {
        Ticket ticket = tickets.findByIdAndBookingCode(ticketId, bookingCode)
                .orElseThrow(() -> AppException.notFound("Khong tim thay ve"));
        if (!ticket.organizerId.equals(organizerId)) {
            throw AppException.forbidden("Ve nay khong thuoc su kien/tour cua ban");
        }
        if (ticket.checkedInAt != null) {
            throw AppException.conflict("Ve da duoc soat truoc do");
        }
        ticket.checkedInAt = Instant.now();
        tickets.save(ticket);
        return "Soat ve thanh cong";
    }

    @Transactional(readOnly = true)
    public OrganizerStat organizerStat(Long organizerId) {
        List<Stock> rows = stocks.findByOrganizerId(organizerId);
        long sold = rows.stream().mapToLong(stock -> stock.sold).sum();
        long quota = rows.stream().mapToLong(stock -> stock.total).sum();
        return new OrganizerStat(sold, quota);
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    @Transactional
    public void expireHolds() {
        List<Hold> rows = holds.findByStatusAndExpiresAtBefore("PENDING", Instant.now());
        for (Hold hold : rows) {
            stocks.release(hold.refType, hold.refId, hold.quantity);
            hold.status = "RELEASED";
            redis.delete(redisKey(hold.bookingCode));
        }
        if (!rows.isEmpty()) {
            holds.saveAll(rows);
            log.info("Da nha {} hold het han", rows.size());
        }
    }

    private void redisTtl(String bookingCode, Instant expiresAt) {
        try {
            Duration ttl = Duration.between(Instant.now(), expiresAt);
            if (!ttl.isNegative() && !ttl.isZero()) {
                redis.opsForValue().set(redisKey(bookingCode), "1", ttl);
            }
        } catch (RuntimeException ex) {
            log.warn("Khong ghi duoc Redis hold: {}", ex.getMessage());
        }
    }

    private static String redisKey(String bookingCode) {
        return "hold:" + bookingCode;
    }

    private StockView view(Stock stock) {
        return new StockView(stock.refType, stock.refId, stock.name, stock.price, stock.total,
                stock.sold, stock.remaining(), stock.maxPerOrder, stock.organizerId);
    }
}
