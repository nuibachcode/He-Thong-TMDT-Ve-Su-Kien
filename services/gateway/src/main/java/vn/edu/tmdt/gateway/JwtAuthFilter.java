package vn.edu.tmdt.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Xac thuc JWT tai bien gioi he thong.
 * - Request public di thang.
 * - Request protected phai co Bearer token hop le.
 * - Gan X-User-Id / X-User-Email / X-User-Role cho service phia sau.
 * - Chan header gia mao do client gui len.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/health",
            "/api/ai/chat",
            "/api/ai/recommendations",
            "/api/ai/health",
            "/api/ai/feedback",
            "/actuator/health",
            "/actuator/info",
            "/v3/api-docs",
            "/swagger-ui.html"
    );

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/public/",
            "/actuator/health/",
            "/v3/api-docs/",
            "/swagger-ui/"
    );

    private final SecretKey key;

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }
        if (isPublic(path)) {
            // API public nhung neu khach da dang nhap thi van chuyen danh tinh
            // de AI gan phien chat / so thich dung nguoi dung.
            if (authorization != null && authorization.startsWith("Bearer ")) {
                try {
                    Claims claims = parse(authorization);
                    return chain.filter(withUserHeaders(exchange, claims));
                } catch (JwtException | IllegalArgumentException ex) {
                    log.debug("Token khong hop le tren API public {}: {}", path, ex.getMessage());
                }
            }
            return chain.filter(stripSpoofedHeaders(exchange));
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Ban can dang nhap de tiep tuc");
        }

        Claims claims;
        try {
            claims = parse(authorization);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT khong hop le tren {}: {}", path, ex.getMessage());
            return error(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Token khong hop le hoac da het han");
        }

        String role = claims.get("role", String.class);
        if (!allowedByRole(path, role)) {
            return error(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Ban khong co quyen thuc hien thao tac nay");
        }

        return chain.filter(withUserHeaders(exchange, claims));
    }

    private Claims parse(String authorization) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(authorization.substring(7).trim())
                .getPayload();
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, Claims claims) {
        Object uid = claims.get("uid");
        String email = claims.getSubject();
        String role = claims.get("role", String.class);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Email");
                    headers.remove("X-User-Role");
                    if (uid != null) {
                        headers.set("X-User-Id", String.valueOf(uid));
                    }
                    if (email != null) {
                        headers.set("X-User-Email", email);
                    }
                    if (role != null) {
                        headers.set("X-User-Role", role);
                    }
                })
                .build();

        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange stripSpoofedHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Email");
                    headers.remove("X-User-Role");
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private boolean isPublic(String path) {
        return PUBLIC_EXACT.contains(path)
                || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean allowedByRole(String path, String role) {
        if (path.startsWith("/api/admin/") || path.startsWith("/api/ai/metrics")
                || path.startsWith("/api/ai/reindex")) {
            return "ADMIN".equals(role);
        }
        if (path.startsWith("/api/organizer/")) {
            return "ORGANIZER".equals(role) || "ADMIN".equals(role);
        }
        return role != null;
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        String json = "{\"success\":false,\"code\":\"" + code + "\",\"message\":\"" + message
                + "\",\"status\":" + status.value() + "}";
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
