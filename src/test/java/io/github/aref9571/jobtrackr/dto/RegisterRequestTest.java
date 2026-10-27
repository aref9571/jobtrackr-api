package io.github.aref9571.jobtrackr.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    @Test
    void toString_hidesPassword() {
        RegisterRequest request = new RegisterRequest("ann@example.com", "secret-password");

        String text = request.toString();

        assertThat(text).contains("ann@example.com");
        assertThat(text).doesNotContain("secret-password");
    }
}
