package ru.uncledrema.gateway.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenResponse {
    @JsonProperty("access_token")
    public String accessToken;
    @JsonProperty("id_token")
    public String idToken;
    @JsonProperty("token_type")
    public String tokenType;
    @JsonProperty("expires_in")
    public Integer expiresIn;
    public String scope;
    @JsonProperty("refresh_token")
    public String refreshToken;
}
