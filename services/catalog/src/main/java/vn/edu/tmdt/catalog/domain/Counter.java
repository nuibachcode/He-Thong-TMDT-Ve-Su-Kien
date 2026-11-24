package vn.edu.tmdt.catalog.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("counters")
public class Counter {

    @Id
    public String id;
    public long seq;
}
