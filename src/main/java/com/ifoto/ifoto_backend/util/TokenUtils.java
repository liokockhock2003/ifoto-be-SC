package com.ifoto.ifoto_backend.util;

import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.UUID;

public final class TokenUtils {

    private TokenUtils() {
    }

    public static String newToken() {
        return UUID.randomUUID().toString();
    }

    public static Instant expiresAt(Instant startTime, long ttlMillis) {
        return startTime.plusMillis(ttlMillis);
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
