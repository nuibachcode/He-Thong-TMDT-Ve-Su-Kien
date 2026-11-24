package vn.edu.tmdt.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "holds")
public class Hold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32)
    public String bookingCode;

    @Column(nullable = false, length = 16)
    public String refType;

    @Column(nullable = false)
    public Long refId;

    @Column(nullable = false, length = 300)
    public String itemName;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal unitPrice;

    @Column(nullable = false)
    public Long organizerId;

    @Column(nullable = false)
    public Instant expiresAt;

    @Column(nullable = false, length = 16)
    public String status = "PENDING";
}
