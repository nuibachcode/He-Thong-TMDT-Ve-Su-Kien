package vn.edu.tmdt.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tmdt.auth.User;
import vn.edu.tmdt.catalog.Event;
import vn.edu.tmdt.catalog.EventRepository;
import vn.edu.tmdt.catalog.TicketType;
import vn.edu.tmdt.catalog.TicketTypeRepository;
import vn.edu.tmdt.catalog.Tour;
import vn.edu.tmdt.catalog.TourRepository;
import vn.edu.tmdt.catalog.TourSchedule;
import vn.edu.tmdt.catalog.TourScheduleRepository;
import vn.edu.tmdt.common.exception.AppException;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int HOLD_MINUTES = 10;

    private final BookingRepository bookings;
    private final BookingItemRepository items;
    private final TicketTypeRepository tickets;
    private final TourScheduleRepository schedules;
    private final EventRepository events;
    private final TourRepository tours;
    private final VoucherRepository vouchers;
    private final PaymentRepository payments;

    public BookingService(BookingRepository bookings, BookingItemRepository items,
                          TicketTypeRepository tickets, TourScheduleRepository schedules,
                          EventRepository events, TourRepository tours,
                          VoucherRepository vouchers, PaymentRepository payments) {
        this.bookings = bookings;
        this.items = items;
        this.tickets = tickets;
        this.schedules = schedules;
        this.events = events;
        this.tours = tours;
        this.vouchers = vouchers;
        this.payments = payments;
    }

    public record ItemInput(Long refId, Integer quantity) {
    }

    @Transactional
    public Booking create(User user, String bookingType, List<ItemInput> inputs, String voucherCode,
                          String contactName, String contactEmail, String contactPhone, String note) {
        if (inputs == null || inputs.isEmpty()) {
            throw AppException.badRequest("Don hang phai co it nhat 1 ve");
        }
        if (!"EVENT".equals(bookingType) && !"TOUR".equals(bookingType)) {
            throw AppException.badRequest("Loai don khong hop le");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<BookingItem> rows = new ArrayList<>();

        for (ItemInput input : inputs) {
            int qty = input.quantity() == null ? 0 : input.quantity();
            if (qty < 1) {
                throw AppException.badRequest("So luong phai lon hon 0");
            }
            if ("EVENT".equals(bookingType)) {
                TicketType tt = tickets.findById(input.refId())
                        .orElseThrow(() -> AppException.notFound("Khong tim thay loai ve"));
                Event event = events.findById(tt.eventId)
                        .orElseThrow(() -> AppException.notFound("Khong tim thay su kien"));
                if (!"PUBLISHED".equals(event.status)) {
                    throw AppException.conflict("Su kien chua mo ban");
                }
                if (tt.maxPerOrder != null && qty > tt.maxPerOrder) {
                    throw AppException.conflict("Moi don chi duoc mua toi da " + tt.maxPerOrder + " ve " + tt.name);
                }
                if (tickets.reserve(tt.id, qty) == 0) {
                    throw AppException.conflict("Loai ve " + tt.name + " chi con " + tt.remaining() + " ve");
                }
                rows.add(row("TICKET", tt.id, event.title + " - " + tt.name, qty, tt.price));
                subtotal = subtotal.add(tt.price.multiply(BigDecimal.valueOf(qty)));
            } else {
                TourSchedule schedule = schedules.findById(input.refId())
                        .orElseThrow(() -> AppException.notFound("Khong tim thay lich khoi hanh"));
                Tour tour = tours.findById(schedule.tourId)
                        .orElseThrow(() -> AppException.notFound("Khong tim thay tour"));
                if (!"OPEN".equals(schedule.status)) {
                    throw AppException.conflict("Lich khoi hanh nay da dong");
                }
                if (schedules.reserve(schedule.id, qty) == 0) {
                    throw AppException.conflict("Lich khoi hanh chi con " + schedule.remaining() + " cho");
                }
                rows.add(row("TOUR", schedule.id,
                        tour.title + " - " + schedule.departureDate, qty, schedule.price));
                subtotal = subtotal.add(schedule.price.multiply(BigDecimal.valueOf(qty)));
            }
        }

        Voucher voucher = resolveVoucher(voucherCode, subtotal);
        BigDecimal discount = voucher == null ? BigDecimal.ZERO : discountOf(voucher, subtotal);

        Booking booking = new Booking();
        booking.code = generateCode();
        booking.userId = user.getId();
        booking.bookingType = bookingType;
        booking.status = "PENDING";
        booking.subtotal = subtotal;
        booking.discountAmount = discount;
        booking.totalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);
        booking.voucherId = voucher == null ? null : voucher.id;
        booking.contactName = contactName == null || contactName.isBlank() ? user.getFullName() : contactName;
        booking.contactEmail = contactEmail == null || contactEmail.isBlank() ? user.getEmail() : contactEmail;
        booking.contactPhone = contactPhone;
        booking.note = note;
        booking.expiresAt = Instant.now().plus(HOLD_MINUTES, ChronoUnit.MINUTES);
        bookings.save(booking);

        for (BookingItem row : rows) {
            row.bookingId = booking.id;
            items.save(row);
        }
        log.info("Tao don {} cho {} ({} {})", booking.code, user.getEmail(), bookingType, inputs.size());
        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> listOf(User user) {
        return bookings.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public Booking requireOwned(User user, String code) {
        return bookings.findByCodeAndUserId(code, user.getId())
                .orElseThrow(() -> AppException.notFound("Khong tim thay don hang"));
    }

    @Transactional(readOnly = true)
    public List<BookingItem> itemsOf(Long bookingId) {
        return items.findByBookingId(bookingId);
    }

    @Transactional
    public Booking cancel(User user, String code) {
        Booking booking = requireOwned(user, code);
        if ("CANCELLED".equals(booking.status) || "EXPIRED".equals(booking.status)) {
            return booking;
        }
        boolean wasConfirmed = "CONFIRMED".equals(booking.status);
        releaseSeats(booking.id);
        if (wasConfirmed && booking.voucherId != null) {
            vouchers.findById(booking.voucherId).ifPresent(v -> v.usedCount = Math.max(0, v.usedCount - 1));
        }
        booking.status = "CANCELLED";
        booking.cancelledAt = Instant.now();
        return booking;
    }

    @Transactional
    public Payment pay(User user, String code) {
        Booking booking = requireOwned(user, code);
        if ("CONFIRMED".equals(booking.status)) {
            return payments.findFirstByBookingIdOrderByPaidAtDesc(booking.id)
                    .orElseThrow(() -> AppException.conflict("Don da xac nhan nhung thieu giao dich"));
        }
        if (!"PENDING".equals(booking.status)) {
            throw AppException.conflict("Don hang khong o trang thai cho thanh toan");
        }
        if (booking.expiresAt != null && booking.expiresAt.isBefore(Instant.now())) {
            throw AppException.conflict("Don hang da het thoi gian giu cho");
        }

        Payment payment = new Payment();
        payment.bookingId = booking.id;
        payment.provider = "MOCK";
        payment.txnRef = "MOCK-" + booking.code + "-" + Long.toHexString(RANDOM.nextLong()).toUpperCase();
        payment.amount = booking.totalAmount;
        payment.status = "SUCCESS";
        payment.paidAt = Instant.now();
        payments.save(payment);

        booking.status = "CONFIRMED";
        booking.confirmedAt = payment.paidAt;
        if (booking.voucherId != null) {
            vouchers.findById(booking.voucherId).ifPresent(v -> v.usedCount = v.usedCount + 1);
        }
        return payment;
    }

    @Transactional
    public int expireOverdue() {
        List<Booking> overdue = bookings.findByStatusAndExpiresAtBefore("PENDING", Instant.now());
        for (Booking booking : overdue) {
            releaseSeats(booking.id);
            booking.status = "EXPIRED";
            booking.cancelledAt = Instant.now();
        }
        return overdue.size();
    }

    private void releaseSeats(Long bookingId) {
        for (BookingItem item : items.findByBookingId(bookingId)) {
            if ("TICKET".equals(item.itemType)) {
                tickets.release(item.refId, item.quantity);
            } else if ("TOUR".equals(item.itemType)) {
                schedules.release(item.refId, item.quantity);
            }
        }
    }

    private BookingItem row(String itemType, Long refId, String name, int qty, BigDecimal unitPrice) {
        BookingItem item = new BookingItem();
        item.itemType = itemType;
        item.refId = refId;
        item.itemName = name;
        item.quantity = qty;
        item.unitPrice = unitPrice;
        item.amount = unitPrice.multiply(BigDecimal.valueOf(qty));
        return item;
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
            discount = subtotal.multiply(voucher.discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = voucher.discountValue;
        }
        if (voucher.maxDiscountAmount != null) {
            discount = discount.min(voucher.maxDiscountAmount);
        }
        return discount.min(subtotal);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder("TKT");
        for (int i = 0; i < 7; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
