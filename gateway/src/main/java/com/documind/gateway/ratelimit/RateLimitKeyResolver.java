package com.documind.gateway.ratelimit;

import com.documind.gateway.ratelimit.RateLimitProperties.KeyType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;

@Component
public class KeyResolver {

    public String resolve(KeyType type, ServerRequest request) {
        return switch (type) {
            case IP -> "ip:" + clientIp(request);
            case USER_OR_IP -> authenticatedUser()
                    .map(id -> "user:" + id)
                    .orElseGet(() -> "ip:" + clientIp(request));
        };
    }

    // Yalnızca doğrulanmış kimlik. Ham Authorization header'ı asla anahtar olmaz.
    private java.util.Optional<String> authenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(auth.getName());
    }

    private String clientIp(ServerRequest request) {
        // forward-headers-strategy: framework ise getRemoteAddr() gerçek istemci IP'sini verir.
        return request.servletRequest() instanceof HttpServletRequest servlet
                ? servlet.getRemoteAddr()
                : "unknown";
    }
}