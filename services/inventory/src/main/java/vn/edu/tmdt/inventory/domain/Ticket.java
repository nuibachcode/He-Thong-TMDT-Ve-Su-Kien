package vn.edu.tmdt.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 32)
    public String bookingCode;

    @Column(nullable = false, length = 300)
    public String itemName;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false, length = 400)
    public String qrPayload;

    @Column(nullable = false)
    public Long organizerId;

    public Instant checkedInAt;
}
