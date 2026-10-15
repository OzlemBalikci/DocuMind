import com.documind.gateway.ratelimit.RateLimitFilters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
public class RouteConfig {

    @Bean
    RouterFunction<ServerResponse> authRoutes(
            RateLimitFilters limits,
            @Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authUrl) {

        return route("auth-login")
                .route(path("/api/auth/login"), http())
                .before(uri(authUrl))
                .filter(limits.of("login"))
                .build()
                .and(route("auth-register")
                        .route(path("/api/auth/register"), http())
                        .before(uri(authUrl))
                        .filter(limits.of("register"))
                        .build())
                .and(route("auth-rest")
                        .route(path("/api/auth/**"), http())
                        .before(uri(authUrl))
                        .build());
    }

    @Bean
    RouterFunction<ServerResponse> chatRoutes(
            RateLimitFilters limits,
            @Value("${CHAT_SERVICE_URL:http://localhost:8083}") String chatUrl) {

        return route("chat")
                .route(path("/api/chat/**"), http())
                .before(uri(chatUrl))
                .filter(limits.of("chat"))
                .build();
    }
}