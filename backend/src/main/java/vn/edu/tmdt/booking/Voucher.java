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
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true)
    public String code;
    public String name;
    public String discountType = "PERCENT";
    @Column(precision = 12, scale = 2)
    public BigDecimal discountValue;
    @Column(precision = 12, scale = 2)
    public BigDecimal minOrderAmount = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2)
    public BigDecimal maxDiscountAmount;
    public Integer usageLimit;
    public Integer usedCount = 0;
    public Instant validFrom;
    public Instant validTo;
    public boolean active = true;
}
