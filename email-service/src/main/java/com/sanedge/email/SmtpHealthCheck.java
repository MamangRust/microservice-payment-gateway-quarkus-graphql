package com.sanedge.email;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Dependency-aware readiness check for SMTP (Phase 5). Performs a real
 * protocol-level round trip (greeting + {@code EHLO}) against the configured
 * mailer host, so readiness reflects actual SMTP reachability.
 */
@Readiness
@ApplicationScoped
@IfBuildProperty(name = "smtp.health.enabled", stringValue = "true")
public class SmtpHealthCheck implements HealthCheck {

    @ConfigProperty(name = "quarkus.mailer.host", defaultValue = "localhost")
    String host;

    @ConfigProperty(name = "quarkus.mailer.port", defaultValue = "587")
    int port;

    @ConfigProperty(name = "smtp.health.timeout.ms", defaultValue = "3000")
    long timeoutMs;

    @Override
    public HealthCheckResponse call() {
        int effectiveTimeout = (int) Math.max(500, timeoutMs);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), effectiveTimeout);
            socket.setSoTimeout(effectiveTimeout);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String banner = reader.readLine();
            if (banner == null || !banner.startsWith("220")) {
                return down("SMTP server did not send a 220 greeting", banner);
            }

            // RFC 5321: EHLO must precede any other command.
            OutputStream out = socket.getOutputStream();
            out.write("EHLO quarkus-health\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            String ehlo = reader.readLine();
            if (ehlo == null || !ehlo.startsWith("250")) {
                return down("SMTP EHLO was rejected", ehlo);
            }

            return HealthCheckResponse.named("smtp-connectivity")
                    .up()
                    .withData("host", host)
                    .withData("port", port)
                    .build();
        } catch (Exception e) {
            return down(e.getMessage(), null);
        }
    }

    private HealthCheckResponse down(String error, String detail) {
        HealthCheckResponseBuilder builder = HealthCheckResponse.named("smtp-connectivity")
                .down()
                .withData("host", host)
                .withData("port", port)
                .withData("error", error == null ? "unknown" : error);
        if (detail != null) {
            builder.withData("detail", detail);
        }
        return builder.build();
    }
}
