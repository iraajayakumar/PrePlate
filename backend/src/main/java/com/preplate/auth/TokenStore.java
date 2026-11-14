package com.preplate.auth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class TokenStore {
    private final ConcurrentHashMap<String, Long> tokens = new ConcurrentHashMap<>();

    public String create(Long userId) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, userId);
        return token;
    }

    public Long userId(String token) {
        return tokens.get(token);
    }

    public void remove(String token) {
        tokens.remove(token);
    }
}
