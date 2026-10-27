package io.github.aref9571.jobtrackr;

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
import org.springframework.test.web.servlet.client.RestTestClient;

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
        restClient.get().uri("/api/applications").exchange().expectStatus().isOk();
    }

    @Test
    void otherPaths_withoutCredentials_areRejected() {
        restClient.get().uri("/actuator").exchange().expectStatus().isForbidden();
    }

    private RestTestClient.ResponseSpec register(String email) {
        return restClient.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, PASSWORD))
                .exchange();
    }
}
