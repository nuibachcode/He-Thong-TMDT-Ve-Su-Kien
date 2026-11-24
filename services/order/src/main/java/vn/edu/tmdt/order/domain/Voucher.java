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
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 40)
    public String code;

    @Column(nullable = false, length = 160)
    public String name;

    @Column(nullable = false, length = 16)
    public String discountType;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal discountValue;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    public BigDecimal maxDiscountAmount;

    public Integer usageLimit;
    public int usedCount;
    public Instant validFrom;
    public Instant validTo;
    public boolean active = true;
}
