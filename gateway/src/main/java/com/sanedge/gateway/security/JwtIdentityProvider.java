package com.sanedge.gateway.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.jwt.Claims;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JwtIdentityProvider implements IdentityProvider<JwtAuthenticationRequest> {

    @Inject
    JWTCallerPrincipalFactory factory;

    @Inject
    JWTAuthContextInfo authContextInfo;

    @Override
    public Class<JwtAuthenticationRequest> getRequestType() {
        return JwtAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(
            JwtAuthenticationRequest request,
            AuthenticationRequestContext context) {

        return Uni.createFrom().item(() -> {
            try {
                JWTCallerPrincipal principal = factory.parse(request.getToken(), authContextInfo);

                QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                        .setPrincipal(principal)
                        .addRoles(principal.getGroups())
                        .addAttribute(Claims.groups.name(), principal.getGroups())
                        .addAttribute(Claims.sub.name(), principal.getSubject());

                Object userId = principal.getClaim("userId");
                if (userId != null) {
                    builder.addAttribute("userId", userId);
                }

                extractRoles(principal).forEach(builder::addRole);

                return builder.build();

            } catch (Exception e) {
                throw new AuthenticationFailedException("Invalid JWT token", e);
            }
        });
    }

    private Set<String> extractRoles(JWTCallerPrincipal principal) {
        Object claim = principal.getClaim("roles");

        if (claim instanceof Set<?> set) {
            return set.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toUnmodifiableSet());
        }

        if (claim instanceof Iterable<?> iterable) {
            Set<String> roles = new java.util.HashSet<>();
            for (Object item : iterable) {
                if (item instanceof String s) {
                    roles.add(s);
                }
            }
            return roles;
        }

        return Set.of();
    }
}
