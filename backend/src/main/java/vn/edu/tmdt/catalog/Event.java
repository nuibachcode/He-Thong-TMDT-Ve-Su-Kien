package vn.edu.tmdt.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long organizerId;
    public Long venueId;
    public String title;
    @Column(unique = true)
    public String slug;
    @Column(columnDefinition = "TEXT")
    public String description;
    public String category;
    public String coverImageUrl;
    public Instant startAt;
    public Instant endAt;
    public Instant saleStartAt;
    public Instant saleEndAt;
    public String status = "PUBLISHED";
    public Instant createdAt = Instant.now();
}
