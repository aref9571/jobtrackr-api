package io.github.aref9571.jobtrackr;

import io.github.aref9571.jobtrackr.config.JwtProperties;
import io.github.aref9571.jobtrackr.config.SecurityConfig;
import io.github.aref9571.jobtrackr.dto.LoginRequest;
import io.github.aref9571.jobtrackr.dto.LoginResponse;
import io.github.aref9571.jobtrackr.dto.RegisterRequest;
import io.github.aref9571.jobtrackr.dto.RegisterResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class AuthApiIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void register_newUser_storesNormalizedEmailAndBcryptHash() {
        RegisterResponse response = restClient.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("Carol@Example.COM", PASSWORD))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(RegisterResponse.class)
                .returnResult()
                .getResponseBody();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select email, password_hash, role from users where id = ?", response.id());

        assertThat(response.email()).isEqualTo("carol@example.com");
        assertThat(row.get("email")).isEqualTo("carol@example.com");
        assertThat(row.get("role")).isEqualTo("USER");
        String passwordHash = (String) row.get("password_hash");
        assertThat(passwordHash).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, passwordHash)).isTrue();
    }

    @Test
    void register_sameEmailInUpperCase_returns409AndKeepsOneUser() {
        register("dave@example.com").expectStatus().isCreated();

        register("DAVE@EXAMPLE.COM")
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Email is already registered");

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where email = ?", Integer.class, "dave@example.com");
        assertThat(count).isEqualTo(1);
    }

    @Test
    void register_validRequest_neverReturnsPasswordOrHash() {
        String body = register("erin@example.com")
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(body).doesNotContain(PASSWORD).doesNotContain("$2").doesNotContainIgnoringCase("password");
    }

    @Test
    void publicPaths_withoutCredentials_areReachable() {
        restClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
        restClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
        restClient.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
    }

    @Test
    void otherPaths_withoutCredentials_areRejected() {
        restClient.get().uri("/actuator").exchange().expectStatus().isUnauthorized();
        restClient.get().uri("/api/applications").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void login_correctCredentials_returnsTokenWithUserIdRolesAndOneHourExpiry() {
        RegisterResponse user = register("gina@example.com").expectBody(RegisterResponse.class)
                .returnResult()
                .getResponseBody();

        LoginResponse response = login("gina@example.com", PASSWORD)
                .expectStatus().isOk()
                .expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        Jwt jwt = jwtDecoder.decode(response.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(user.id().toString());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void login_emailInOtherCase_returnsToken() {
        register("hank@example.com").expectStatus().isCreated();

        login("HANK@Example.com", PASSWORD).expectStatus().isOk();
    }

    @Test
    void login_wrongPasswordAndUnknownEmail_returnSame401Response() {
        register("ivy@example.com").expectStatus().isCreated();

        String wrongPassword = login("ivy@example.com", "wrong-password")
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        String unknownEmail = login("nobody@example.com", PASSWORD)
                .expectStatus().isUnauthorized()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(wrongPassword).contains("Invalid email or password");
        assertThat(unknownEmail).isEqualTo(wrongPassword);
    }

    @Test
    void applications_withValidToken_returns200() {
        register("jack@example.com").expectStatus().isCreated();
        String token = login("jack@example.com", PASSWORD).expectBody(LoginResponse.class)
                .returnResult()
                .getResponseBody()
                .accessToken();

        getApplications(token).expectStatus().isOk();
    }

    @Test
    void applications_withBrokenToken_returns401() {
        getApplications("not-a-jwt")
                .expectStatus().isUnauthorized()
                .expectHeader().value("WWW-Authenticate", value -> assertThat(value).contains("invalid_token"));
    }

    @Test
    void applications_withExpiredToken_returns401() {
        Instant issuedAt = Instant.now().minus(Duration.ofHours(2));
        String token = signedToken(jwtEncoder, issuedAt, issuedAt.plus(Duration.ofHours(1)));

        getApplications(token).expectStatus().isUnauthorized();
    }

    @Test
    void applications_withTokenSignedByOtherSecret_returns401() {
        JwtProperties otherProperties = new JwtProperties("another-secret-that-the-server-does-not-know", Duration.ofHours(1));
        JwtEncoder otherEncoder = new SecurityConfig().jwtEncoder(otherProperties);
        String token = signedToken(otherEncoder, Instant.now(), Instant.now().plus(Duration.ofHours(1)));

        getApplications(token).expectStatus().isUnauthorized();
    }

    private RestTestClient.ResponseSpec getApplications(String token) {
        return restClient.get().uri("/api/applications")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange();
    }

    private static String signedToken(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("7c9e6679-7425-40de-944b-e07fc1f90ae7")
                .claim("roles", List.of("USER"))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private RestTestClient.ResponseSpec login(String email, String password) {
        return restClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, password))
                .exchange();
    }

    private RestTestClient.ResponseSpec register(String email) {
        return restClient.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, PASSWORD))
                .exchange();
    }
}
