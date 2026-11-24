package vn.edu.tmdt.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tours")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long organizerId;
    public String title;
    @Column(unique = true)
    public String slug;
    @Column(columnDefinition = "LONGTEXT")
    public String description;
    public String destination;
    public String departureCity;
    public Integer durationDays;
    public Integer durationNights = 0;
    @Column(precision = 12, scale = 2)
    public BigDecimal basePrice;
    public String coverImageUrl;
    @Column(columnDefinition = "LONGTEXT")
    public String highlights;
    @Column(columnDefinition = "LONGTEXT")
    public String included;
    @Column(columnDefinition = "LONGTEXT")
    public String excluded;
    public String status = "PUBLISHED";
    public Instant createdAt = Instant.now();
}
