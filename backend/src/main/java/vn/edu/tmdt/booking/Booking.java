package vn.edu.tmdt.booking;

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

    @Column(unique = true, length = 20)
    public String code;
    public Long userId;
    public String bookingType;
    public String status = "PENDING";
    @Column(precision = 12, scale = 2)
    public BigDecimal subtotal = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2)
    public BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2)
    public BigDecimal totalAmount = BigDecimal.ZERO;
    public Long voucherId;
    public String contactName;
    public String contactEmail;
    public String contactPhone;
    public String note;
    public Instant expiresAt;
    public Instant confirmedAt;
    public Instant cancelledAt;
    public Instant createdAt = Instant.now();
}
