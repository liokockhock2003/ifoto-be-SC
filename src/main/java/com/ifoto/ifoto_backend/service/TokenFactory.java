package com.ifoto.ifoto_backend.service;

import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.UUID;

public final class TokenFactory {

    private TokenFactory() {
    }

    public static String newToken() {
        return UUID.randomUUID().toString();
    }

    public static Instant expiresAt(long ttlMillis) {
        return Instant.now().plusMillis(ttlMillis);
    }

    public static String buildLink(String baseUrl, String token) {
        return UriComponentsBuilder
                .fromUriString(baseUrl)
                .queryParam("token", token)
                .build()
                .encode()
                .toUriString();
    }
}
