package vn.edu.tmdt.ai.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_interests")
public class UserInterests {

    @Id
    public Long userId;
    public List<String> interests = new ArrayList<>();
    public Instant updatedAt;
}
