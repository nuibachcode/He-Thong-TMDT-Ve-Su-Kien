package vn.edu.tmdt.catalog.domain;

import java.math.BigDecimal;

public class TicketTypeDef {

    public Long id;
    public String name;
    public BigDecimal price;
    public int quota;
    public int maxPerOrder = 4;
}
