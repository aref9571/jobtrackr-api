package io.github.aref9571.jobtrackr.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuuS7Ir1Dx5l6xNQ7r5BkcGcCL6kK1yJ0O";

    @Test
    void constructor_validValues_createsUser() {
        User user = new User("ann@example.com", PASSWORD_HASH, Role.USER);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("ann@example.com");
        assertThat(user.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void constructor_blankEmail_throwsException() {
        assertThatThrownBy(() -> new User(" ", PASSWORD_HASH, Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");
    }

    @Test
    void constructor_missingPasswordHash_throwsException() {
        assertThatThrownBy(() -> new User("ann@example.com", null, Role.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash must not be blank");
    }

    @Test
    void constructor_missingRole_throwsException() {
        assertThatThrownBy(() -> new User("ann@example.com", PASSWORD_HASH, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role must not be null");
    }

    @Test
    void toString_neverContainsPasswordHash() {
        User user = new User("ann@example.com", PASSWORD_HASH, Role.USER);

        String text = user.toString();

        assertThat(text).doesNotContain(PASSWORD_HASH);
    }
}
