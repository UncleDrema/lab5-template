package ru.uncledrema.flights.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class AuthRestTemplateConfiguration {
    @Bean
    public RestTemplate restTemplateWithAuthForwarding() {
        RestTemplate rt = new RestTemplate();

        rt.getInterceptors().add((request, body, execution) -> {
            // сначала пробуем взять Authorization из текущего HttpServletRequest
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest current = attrs.getRequest();
                String auth = current.getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null && !auth.isBlank()) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, auth);
                    return execution.execute(request, body);
                }
            }

            // fallback: если нет HttpServletRequest — попробуем взять из SecurityContext (JwtAuthenticationToken)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                Jwt token = jwtAuthenticationToken.getToken();
                if (token != null && token.getTokenValue() != null) {
                    request.getHeaders().setBearerAuth(token.getTokenValue());
                }
            }

            return execution.execute(request, body);
        });

        return rt;
    }
}
