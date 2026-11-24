package vn.edu.tmdt.catalog.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ScheduleDef {

    public Long id;
    public LocalDate departureDate;
    public LocalDate returnDate;
    public BigDecimal price;
    public int seatsTotal;
}
