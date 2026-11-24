package vn.edu.tmdt.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false)
    public Long userId;

    @Column(nullable = false, length = 16)
    public String bookingType;

    @Column(nullable = false, length = 24)
    public String status = "PENDING";

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    public Long voucherId;

    @Column(length = 160)
    public String contactName;

    @Column(length = 160)
    public String contactEmail;

    @Column(length = 32)
    public String contactPhone;

    @Column(columnDefinition = "TEXT")
    public String note;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public Instant confirmedAt;
    public Instant cancelledAt;
    public Instant expiresAt;
}
