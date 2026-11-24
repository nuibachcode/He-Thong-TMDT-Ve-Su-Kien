package vn.edu.tmdt.ai.domain;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "event_cards")
public class EventCard {

    @Id
    public Long id;
    public String title;
    public String slug;
    public String category;
    public String description;
    public String venue;
    public String city;
    public String startAt;
    public long priceFrom;
    public String coverImageUrl;
    public List<String> tags = new ArrayList<>();
    public int popularity;
}
