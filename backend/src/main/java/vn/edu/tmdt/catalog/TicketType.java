package vn.edu.tmdt.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_types")
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long eventId;
    public String name;
    public String description;
    @Column(precision = 12, scale = 2)
    public BigDecimal price;
    public Integer quota;
    public Integer soldCount = 0;
    public Integer maxPerOrder = 5;

    public int remaining() {
        return Math.max(0, quota - soldCount);
    }
}
