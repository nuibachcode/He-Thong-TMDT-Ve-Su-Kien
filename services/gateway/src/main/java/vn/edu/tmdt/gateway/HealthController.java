package vn.edu.tmdt.gateway;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
                "success", true,
                "message", "OK",
                "data", Map.of(
                        "status", "UP",
                        "app", "ticketgo-gateway",
                        "time", Instant.now().toString())));
    }
}
