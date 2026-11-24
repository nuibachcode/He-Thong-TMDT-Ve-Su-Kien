package vn.edu.tmdt.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(name = "stocks", uniqueConstraints = @UniqueConstraint(columnNames = {"ref_type", "ref_id"}))
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "ref_type", nullable = false, length = 16)
    public String refType;

    @Column(name = "ref_id", nullable = false)
    public Long refId;

    @Column(nullable = false, length = 300)
    public String name;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal price;

    @Column(nullable = false)
    public int total;

    @Column(nullable = false)
    public int sold;

    @Column(nullable = false)
    public int maxPerOrder = 4;

    @Column(name = "organizer_id")
    public Long organizerId;

    public int remaining() {
        return Math.max(0, total - sold);
    }
}
