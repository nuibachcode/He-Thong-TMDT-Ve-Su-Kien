package vn.edu.tmdt.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.common.ApiResponse;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Value("${spring.application.name:app}")
    private String appName;

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("app", appName);
        body.put("time", Instant.now().toString());
        return ApiResponse.ok(body);
    }
}
