package io.github.aref9571.jobtrackr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties("jobtrackr.jwt")
public record JwtProperties(String secret, Duration expiration) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("jobtrackr.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalArgumentException("jobtrackr.jwt.expiration must be positive");
        }
    }
}
