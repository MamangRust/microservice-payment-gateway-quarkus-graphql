package com.sanedge.gateway.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.security.identity.request.AuthenticationRequest;

public class JwtAuthenticationRequest implements AuthenticationRequest {

    private final String token;
    private final Map<String, Object> attributes;

    public JwtAuthenticationRequest(String token) {
        this.token = token;
        this.attributes = new HashMap<>();
    }

    public String getToken() {
        return token;
    }

    @Override
    public <T> T getAttribute(String name) {
        @SuppressWarnings("unchecked")
        T value = (T) attributes.get(name);
        return value;
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
