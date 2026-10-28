package io.github.aref9571.jobtrackr.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    @Test
    void toString_hidesPassword() {
        LoginRequest request = new LoginRequest("ann@example.com", "secret-password");

        String text = request.toString();

        assertThat(text).contains("ann@example.com");
        assertThat(text).doesNotContain("secret-password");
    }
}
