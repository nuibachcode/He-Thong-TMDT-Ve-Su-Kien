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
@Table(name = "booking_items")
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long bookingId;
    public String itemType;
    public Long refId;
    public String itemName;
    public Integer quantity;
    @Column(precision = 12, scale = 2)
    public BigDecimal unitPrice;
    @Column(precision = 12, scale = 2)
    public BigDecimal amount;
    public Instant checkedInAt;
}
