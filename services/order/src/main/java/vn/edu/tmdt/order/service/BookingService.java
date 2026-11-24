package vn.edu.tmdt.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.order.client.CatalogClient;
import vn.edu.tmdt.order.client.IdentityClient;
import vn.edu.tmdt.order.client.InventoryClient;
import vn.edu.tmdt.order.common.AppException;
import vn.edu.tmdt.order.config.RabbitConfig;
import vn.edu.tmdt.order.domain.Booking;
import vn.edu.tmdt.order.domain.BookingItem;
import vn.edu.tmdt.order.domain.Voucher;
import vn.edu.tmdt.order.repository.BookingItemRepository;
import vn.edu.tmdt.order.repository.BookingRepository;
import vn.edu.tmdt.order.repository.VoucherRepository;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final int HOLD_MINUTES = 10;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    public record ItemInput(Long refId, Integer quantity) {
    }

    public record ItemView(Long id, String itemName, Integer quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    public record Summary(String code, String bookingType, String status, BigDecimal totalAmount,
                          Instant createdAt, Instant expiresAt, int itemCount) {
    }

    public record Detail(String code, String bookingType, String status, BigDecimal subtotal,
                         BigDecimal discountAmount, BigDecimal totalAmount, String contactName,
                         String contactEmail, String contactPhone, String note, Instant createdAt,
                         Instant confirmedAt, Instant expiresAt, List<ItemView> items) {
    }

    public record OrgBooking(String code, String contactName, String contactEmail, String status,
                             BigDecimal totalAmount, Instant createdAt, String itemSummary) {
    }

    public record OrgStats(BigDecimal revenue, long orders, long ticketsSold, double fillRate,
                           long pendingEvents, long pendingTours) {
    }

    public record AdminStats(long users, long events, long tours, long bookings, BigDecimal revenue) {
    }

    public record InternalBooking(String code, Long userId, String status, BigDecimal totalAmount,
                                  Instant expiresAt, Instant confirmedAt) {
    }

    private final BookingRepository bookings;
    private final BookingItemRepository items;
    private final VoucherRepository vouchers;
    private final InventoryClient inventory;
    private final CatalogClient catalog;
    private final IdentityClient identity;
    private final RabbitTemplate rabbit;

    public BookingService(BookingRepository bookings, BookingItemRepository items, VoucherRepository vouchers,
                          InventoryClient inventory, CatalogClient catalog, IdentityClient identity,
                          RabbitTemplate rabbit) {
        this.bookings = bookings;
        this.items = items;
        this.vouchers = vouchers;
        this.inventory = inventory;
        this.catalog = catalog;
        this.identity = identity;
        this.rabbit = rabbit;
    }

    @Transactional
    public Summary create(Long userId, String email, String bookingType, List<ItemInput> input,
                          String voucherCode, String contactName, String contactEmail, String contactPhone,
                          String note) {
        if (userId == null) {
            throw AppException.unauthorized("Ban can dang nhap");
        }
        if (input == null || input.isEmpty()) {
            throw AppException.badRequest("Don hang phai co it nhat 1 ve");
        }
        if (!"EVENT".equals(bookingType) && !"TOUR".equals(bookingType)) {
            throw AppException.badRequest("Loai don khong hop le");
        }
        String code = generateCode();
        Instant expiresAt = Instant.now().plus(HOLD_MINUTES, ChronoUnit.MINUTES);
        String refType = "EVENT".equals(bookingType) ? "TICKET" : "SCHEDULE";
        List<Long> refIds = input.stream().map(ItemInput::refId).distinct().toList();
        List<CatalogClient.ItemMeta> metas = catalog.items(refType, refIds);
        if (metas.size() < refIds.size()
                || metas.stream().anyMatch(meta -> !"PUBLISHED".equals(meta.status()))) {
            throw AppException.conflict("Su kien/tour chua mo ban hoac khong ton tai");
        }
        List<InventoryClient.HoldItem> holdItems = input.stream()
                .map(item -> new InventoryClient.HoldItem(refType, item.refId(), item.quantity()))
                .toList();

        InventoryClient.HoldResult held = inventory.hold(code, expiresAt, holdItems);
        try {
            BigDecimal subtotal = held.total();
            Voucher voucher = resolveVoucher(voucherCode, subtotal);
            BigDecimal discount = voucher == null ? BigDecimal.ZERO : discountOf(voucher, subtotal);
            Booking booking = new Booking();
            booking.code = code;
            booking.userId = userId;
            booking.bookingType = bookingType;
            booking.status = "PENDING";
            booking.subtotal = subtotal;
            booking.discountAmount = discount;
            booking.totalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);
            booking.voucherId = voucher == null ? null : voucher.id;
            booking.contactName = contactName == null || contactName.isBlank() ? email : contactName;
            booking.contactEmail = contactEmail == null || contactEmail.isBlank() ? email : contactEmail;
            booking.contactPhone = contactPhone;
            booking.note = note;
            booking.expiresAt = expiresAt;
            bookings.save(booking);
            for (InventoryClient.HoldLine line : held.items()) {
                BookingItem row = new BookingItem();
                row.bookingId = booking.id;
                row.itemType = "TICKET".equals(line.refType()) ? "TICKET" : "TOUR";
                row.refId = line.refId();
                row.itemName = line.itemName();
                row.quantity = line.quantity();
                row.unitPrice = line.unitPrice();
                row.amount = line.amount();
                row.organizerId = line.organizerId() == null ? 0L : line.organizerId();
                items.save(row);
            }
            publish("order.created", code);
            return summary(booking);
        } catch (RuntimeException ex) {
            inventory.release(code);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<Summary> listOf(Long userId) {
        if (userId == null) {
            throw AppException.unauthorized("Ban can dang nhap");
        }
        return bookings.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public Detail detail(Long userId, String code) {
        Booking booking = requireOwned(userId, code);
        return detailOf(booking);
    }

    @Transactional
    public Summary cancel(Long userId, String code) {
        Booking booking = requireOwned(userId, code);
        if ("CANCELLED".equals(booking.status) || "EXPIRED".equals(booking.status)) {
            return summary(booking);
        }
        boolean confirmed = "CONFIRMED".equals(booking.status);
        inventory.release(code);
        if (confirmed && booking.voucherId != null) {
            vouchers.findById(booking.voucherId).ifPresent(voucher ->
                    voucher.usedCount = Math.max(0, voucher.usedCount - 1));
        }
        booking.status = "CANCELLED";
        booking.cancelledAt = Instant.now();
        bookings.save(booking);
        publish("order.cancelled", code);
        return summary(booking);
    }

    @Transactional(readOnly = true)
    public List<InventoryClient.TicketView> tickets(Long userId, String code) {
        requireOwned(userId, code);
        return inventory.tickets(code);
    }

    @Transactional(readOnly = true)
    public InternalBooking internalGet(String code) {
        Booking booking = bookings.findByCode(code)
                .orElseThrow(() -> AppException.notFound("Khong tim thay don hang"));
        return new InternalBooking(booking.code, booking.userId, booking.status, booking.totalAmount,
                booking.expiresAt, booking.confirmedAt);
    }

    @Transactional
    public InternalBooking internalConfirm(String code, Long userId, String txnRef, String provider) {
        Booking booking = bookings.findByCode(code)
                .orElseThrow(() -> AppException.notFound("Khong tim thay don hang"));
        if (userId != null && !booking.userId.equals(userId)) {
            throw AppException.unauthorized("Khong phai chu don hang");
        }
        if ("CONFIRMED".equals(booking.status)) {
            return internalGet(code);
        }
        if (!"PENDING".equals(booking.status)) {
            throw AppException.conflict("Don hang khong o trang thai cho thanh toan");
        }
        if (booking.expiresAt != null && booking.expiresAt.isBefore(Instant.now())) {
            throw AppException.conflict("Don hang da het thoi gian giu cho");
        }
        inventory.confirm(code);
        booking.status = "CONFIRMED";
        booking.confirmedAt = Instant.now();
        if (booking.voucherId != null) {
            vouchers.findById(booking.voucherId).ifPresent(voucher -> voucher.usedCount = voucher.usedCount + 1);
        }
        bookings.save(booking);
        publish("order.confirmed", code + "|" + (txnRef == null ? "" : txnRef) + "|" + (provider == null ? "MOCK" : provider));
        log.info("Don {} da thanh toan qua {}", code, provider);
        return internalGet(code);
    }

    @Transactional(readOnly = true)
    public List<OrgBooking> organizerBookings(Long organizerId) {
        List<OrgBooking> result = new ArrayList<>();
        for (Booking booking : bookings.findAll()) {
            List<BookingItem> rows = items.findByBookingId(booking.id).stream()
                    .filter(item -> item.organizerId.equals(organizerId)).toList();
            if (rows.isEmpty()) {
                continue;
            }
            String summary = rows.stream().map(item -> item.itemName + " x" + item.quantity)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            result.add(new OrgBooking(booking.code, booking.contactName, booking.contactEmail,
                    booking.status, booking.totalAmount, booking.createdAt, summary));
        }
        result.sort(Comparator.comparing(OrgBooking::createdAt).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public OrgStats organizerStats(Long organizerId) {
        Set<Long> orderIds = new HashSet<>();
        BigDecimal revenue = BigDecimal.ZERO;
        long ticketsSold = 0;
        for (Booking booking : bookings.findByStatus("CONFIRMED")) {
            for (BookingItem item : items.findByBookingId(booking.id)) {
                if (!item.organizerId.equals(organizerId)) {
                    continue;
                }
                orderIds.add(booking.id);
                revenue = revenue.add(item.amount);
                ticketsSold += item.quantity;
            }
        }
        InventoryClient.OrganizerStat stock = inventory.organizerStat(organizerId);
        double fillRate = stock.quota() == 0 ? 0 : Math.round((double) stock.sold() / stock.quota() * 1000) / 1000.0;
        Map<String, Long> pending = catalog.pendingCounts(organizerId);
        return new OrgStats(revenue.setScale(2, RoundingMode.HALF_UP), orderIds.size(), ticketsSold,
                fillRate, pending.getOrDefault("pendingEvents", 0L),
                pending.getOrDefault("pendingTours", 0L));
    }

    @Transactional(readOnly = true)
    public AdminStats adminStats() {
        BigDecimal revenue = bookings.findByStatus("CONFIRMED").stream()
                .map(booking -> booking.totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> counts = catalog.counts();
        return new AdminStats(identity.usersCount(), counts.getOrDefault("events", 0L),
                counts.getOrDefault("tours", 0L), bookings.count(), revenue);
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void expireBookings() {
        List<Booking> expired = bookings.findByStatusAndExpiresAtBefore("PENDING", Instant.now());
        for (Booking booking : expired) {
            inventory.release(booking.code);
            booking.status = "EXPIRED";
            booking.cancelledAt = Instant.now();
            bookings.save(booking);
            publish("order.expired", booking.code);
        }
        if (!expired.isEmpty()) {
            log.info("Da het han {} don giu cho", expired.size());
        }
    }

    private Booking requireOwned(Long userId, String code) {
        if (userId == null) {
            throw AppException.unauthorized("Ban can dang nhap");
        }
        return bookings.findByCodeAndUserId(code, userId)
                .orElseThrow(() -> AppException.notFound("Khong tim thay don hang"));
    }

    private Detail detailOf(Booking booking) {
        List<ItemView> rows = items.findByBookingId(booking.id).stream()
                .map(item -> new ItemView(item.id, item.itemName, item.quantity, item.unitPrice, item.amount))
                .toList();
        return new Detail(booking.code, booking.bookingType, booking.status, booking.subtotal,
                booking.discountAmount, booking.totalAmount, booking.contactName, booking.contactEmail,
                booking.contactPhone, booking.note, booking.createdAt, booking.confirmedAt,
                booking.expiresAt, rows);
    }

    private Summary summary(Booking booking) {
        return new Summary(booking.code, booking.bookingType, booking.status, booking.totalAmount,
                booking.createdAt, booking.expiresAt, items.findByBookingId(booking.id).size());
    }

    private Voucher resolveVoucher(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Voucher voucher = vouchers.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> AppException.badRequest("Ma giam gia khong ton tai"));
        Instant now = Instant.now();
        if (!voucher.active) {
            throw AppException.conflict("Ma giam gia da bi khoa");
        }
        if (voucher.validFrom != null && now.isBefore(voucher.validFrom)) {
            throw AppException.conflict("Ma giam gia chua co hieu luc");
        }
        if (voucher.validTo != null && now.isAfter(voucher.validTo)) {
            throw AppException.conflict("Ma giam gia da het han");
        }
        if (voucher.usageLimit != null && voucher.usedCount >= voucher.usageLimit) {
            throw AppException.conflict("Ma giam gia da het luot su dung");
        }
        if (subtotal.compareTo(voucher.minOrderAmount) < 0) {
            throw AppException.conflict("Don toi thieu " + voucher.minOrderAmount + " de dung ma nay");
        }
        return voucher;
    }

    private BigDecimal discountOf(Voucher voucher, BigDecimal subtotal) {
        BigDecimal discount;
        if ("PERCENT".equals(voucher.discountType)) {
            discount = subtotal.multiply(voucher.discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = voucher.discountValue;
        }
        if (voucher.maxDiscountAmount != null) {
            discount = discount.min(voucher.maxDiscountAmount);
        }
        return discount.min(subtotal);
    }

    private static String generateCode() {
        StringBuilder sb = new StringBuilder("TKT");
        for (int i = 0; i < 7; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private void publish(String routingKey, String payload) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, routingKey, payload);
        } catch (RuntimeException ex) {
            log.warn("Khong gui duoc su kien {}: {}", routingKey, ex.getMessage());
        }
    }
}
