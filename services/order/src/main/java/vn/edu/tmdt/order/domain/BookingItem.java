package vn.edu.tmdt.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_items")
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public Long bookingId;

    @Column(nullable = false, length = 16)
    public String itemType;

    @Column(nullable = false)
    public Long refId;

    @Column(nullable = false, length = 300)
    public String itemName;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false)
    public Long organizerId;
}
