package vn.edu.tmdt.ai.domain;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tour_cards")
public class TourCard {

    @Id
    public Long id;
    public String title;
    public String slug;
    public String destination;
    public String departureCity;
    public int durationDays;
    public int durationNights;
    public long basePrice;
    public String coverImageUrl;
    public List<String> tags = new ArrayList<>();
    public int popularity;
}
