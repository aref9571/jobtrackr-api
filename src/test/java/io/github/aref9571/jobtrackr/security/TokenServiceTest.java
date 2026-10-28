package io.github.aref9571.jobtrackr.security;

import io.github.aref9571.jobtrackr.config.JwtProperties;
import io.github.aref9571.jobtrackr.config.SecurityConfig;
import io.github.aref9571.jobtrackr.dto.LoginResponse;
import io.github.aref9571.jobtrackr.model.Role;
import io.github.aref9571.jobtrackr.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TokenServiceTest {

    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuuS7Ir1Dx5l6xNQ7r5BkcGcCL6kK1yJ0O";

    private final JwtProperties jwtProperties =
            new JwtProperties("test-only-jwt-secret-for-signing-tokens", Duration.ofHours(1));
    private final SecurityConfig securityConfig = new SecurityConfig();
    private final TokenService tokenService = new TokenService(securityConfig.jwtEncoder(jwtProperties), jwtProperties);
    private final JwtDecoder jwtDecoder = securityConfig.jwtDecoder(jwtProperties);

    @Test
    void createToken_user_returnsBearerTokenWithExpirationInSeconds() {
        User user = new User("ann@example.com", PASSWORD_HASH, Role.USER);

        LoginResponse response = tokenService.createToken(user);

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.accessToken().split("\\.")).hasSize(3);
    }

    @Test
    void createToken_user_signsHs256TokenWithUserIdRolesAndTimestamps() {
        User user = new User("ann@example.com", PASSWORD_HASH, Role.ADMIN);

        Jwt jwt = jwtDecoder.decode(tokenService.createToken(user).accessToken());

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getIssuedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(jwt.getClaims()).doesNotContainKey("email");
    }
}
