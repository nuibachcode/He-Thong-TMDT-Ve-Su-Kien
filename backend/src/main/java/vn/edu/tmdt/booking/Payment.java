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
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long bookingId;
    public String provider = "MOCK";
    @Column(unique = true)
    public String txnRef;
    @Column(precision = 12, scale = 2)
    public BigDecimal amount;
    public String status = "SUCCESS";
    public Instant paidAt = Instant.now();
}
