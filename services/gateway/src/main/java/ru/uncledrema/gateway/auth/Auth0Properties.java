package ru.uncledrema.gateway.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "auth.auth0")
public class Auth0Properties {

    /**
     * your-tenant.auth0.com
     */
    private String domain;

    private String clientId;

    private String clientSecret;

    /**
     * API Identifier (audience)
     */
    private String audience;

    private String redirectUri;
}
