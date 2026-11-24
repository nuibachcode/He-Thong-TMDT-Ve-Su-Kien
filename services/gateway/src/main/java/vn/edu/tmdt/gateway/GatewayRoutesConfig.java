package vn.edu.tmdt.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dinh tuyen theo bounded context. Thu tu route duoc giu theo thu tu khai bao:
 * route cu the phai dung truoc route tong quat.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${app.services.identity-uri}") String identity,
                               @Value("${app.services.catalog-uri}") String catalog,
                               @Value("${app.services.inventory-uri}") String inventory,
                               @Value("${app.services.order-uri}") String order,
                               @Value("${app.services.payment-uri}") String payment,
                               @Value("${app.services.ai-uri}") String ai) {
        return builder.routes()
                .route("ai", route -> route.path("/api/ai/**").uri(ai))
                .route("identity", route -> route.path("/api/auth/**").uri(identity))
                .route("admin-users", route -> route
                        .path("/api/admin/users", "/api/admin/users/**").uri(identity))
                .route("catalog-admin", route -> route
                        .path("/api/admin/pending", "/api/admin/content/**").uri(catalog))
                .route("order-admin-stats", route -> route
                        .path("/api/admin/stats/**").uri(order))
                .route("inventory-checkin", route -> route
                        .path("/api/organizer/checkin").uri(inventory))
                .route("organizer-ai", route -> route
                        .path("/api/organizer/ai/**").uri(ai))
                .route("organizer-order", route -> route
                        .path("/api/organizer/bookings", "/api/organizer/stats/**").uri(order))
                .route("organizer-catalog", route -> route
                        .path("/api/organizer/events", "/api/organizer/events/**",
                                "/api/organizer/tours", "/api/organizer/tours/**").uri(catalog))
                .route("public-catalog", route -> route.path("/api/public/**").uri(catalog))
                .route("bookings", route -> route.path("/api/bookings/**").uri(order))
                .route("payments", route -> route.path("/api/payments/**").uri(payment))
                .build();
    }
}
