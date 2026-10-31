package io.github.aref9571.jobtrackr;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
        "DB_URL=jdbc:postgresql://prod-db.example.com:5432/jobtrackr",
        "DB_USERNAME=prod-user",
        "DB_PASSWORD=prod-password"
})
@ActiveProfiles("prod")
@AutoConfigureRestTestClient
class ProductionProfileIntegrationTest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private Environment environment;

    @Test
    void datasourceSettings_inProdProfile_comeFromEnvironmentVariablesWithoutDefaults() throws IOException {
        PropertySource<?> prodSettings = new YamlPropertySourceLoader()
                .load("prod", new ClassPathResource("application-prod.yml"))
                .getFirst();

        assertThat(prodSettings.getProperty("spring.datasource.url")).hasToString("${DB_URL}");
        assertThat(prodSettings.getProperty("spring.datasource.username")).hasToString("${DB_USERNAME}");
        assertThat(prodSettings.getProperty("spring.datasource.password")).hasToString("${DB_PASSWORD}");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("prod-user");
    }

    @Test
    void health_inProdProfile_showsOnlyStatus() {
        restClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components").doesNotExist()
                .jsonPath("$.details").doesNotExist();
    }

    @Test
    void otherActuatorEndpoints_inProdProfile_areNotPublic() {
        restClient.get().uri("/actuator/env").exchange().expectStatus().isUnauthorized();
        restClient.get().uri("/actuator/info").exchange().expectStatus().isUnauthorized();
    }
}
