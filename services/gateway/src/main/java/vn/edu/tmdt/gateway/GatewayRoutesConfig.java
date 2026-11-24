package vn.edu.tmdt.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dinh tuyen:
 *  - /api/ai/**  -> AI service (RAG + recommendation)
 *  - /api/**     -> core-api (composite service tam thoi: identity, catalog, inventory, order, payment)
 *
 * Khi tach cac service cua Thanh/Dat, chi can them route moi o day.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               @Value("${app.services.core-uri}") String coreUri,
                               @Value("${app.services.ai-uri}") String aiUri) {
        return builder.routes()
                .route("ai-service", route -> route
                        .path("/api/ai/**")
                        .uri(aiUri))
                .route("core-api", route -> route
                        .path("/api/**")
                        .uri(coreUri))
                .build();
    }
}
