package vn.edu.tmdt.order.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class IdentityClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityClient.class);

    private final RestClient http;

    public IdentityClient(RestClient.Builder builder,
                          @Value("${app.services.identity-url}") String identityUrl) {
        this.http = builder.baseUrl(identityUrl).build();
    }

    public long usersCount() {
        try {
            Map<String, Long> body = http.get().uri("/internal/users/count").retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Long>>() {
                    });
            return body == null ? 0 : body.getOrDefault("users", 0L);
        } catch (RestClientException ex) {
            log.warn("Khong lay duoc user count: {}", ex.getMessage());
            return 0;
        }
    }
}
