package vn.edu.tmdt.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String bookingCode;

    @Column(nullable = false)
    public Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false, length = 24)
    public String provider = "MOCK";

    @Column(nullable = false, unique = true, length = 80)
    public String txnRef;

    @Column(nullable = false, length = 20)
    public String status = "PENDING";

    public Instant paidAt;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();
}
