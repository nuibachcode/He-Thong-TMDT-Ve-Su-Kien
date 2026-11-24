package vn.edu.tmdt.catalog.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("venues")
public class Venue {

    @Id
    public Long id;
    public String name;
    public String address;
    public String city;
    public int capacity;
}
