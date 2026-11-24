package vn.edu.tmdt.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tour_schedules")
public class TourSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long tourId;
    public LocalDate departureDate;
    public LocalDate returnDate;
    @Column(precision = 12, scale = 2)
    public BigDecimal price;
    public Integer seatsTotal;
    public Integer seatsBooked = 0;
    public String status = "OPEN";

    public int remaining() {
        return Math.max(0, seatsTotal - seatsBooked);
    }
}
