package ru.uncledrema.gateway.auth;

public record LoginResult(
    String accessToken,
    String tokenType
) { }
