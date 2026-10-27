package io.github.aref9571.jobtrackr.repository;

import io.github.aref9571.jobtrackr.TestcontainersConfiguration;
import io.github.aref9571.jobtrackr.model.Role;
import io.github.aref9571.jobtrackr.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuuS7Ir1Dx5l6xNQ7r5BkcGcCL6kK1yJ0O";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAndFlush_newUser_storesAllFields() {
        User user = userRepository.saveAndFlush(new User("ann@example.com", PASSWORD_HASH, Role.USER));
        entityManager.clear();

        User loaded = userRepository.findById(user.getId()).orElseThrow();

        assertThat(loaded.getEmail()).isEqualTo("ann@example.com");
        assertThat(loaded.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(loaded.getRole()).isEqualTo(Role.USER);
        assertThat(loaded.getCreatedAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    void existsByEmail_registeredAndUnknownEmail_returnsTrueOnlyForRegistered() {
        userRepository.saveAndFlush(new User("ann@example.com", PASSWORD_HASH, Role.USER));

        assertThat(userRepository.existsByEmail("ann@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("bob@example.com")).isFalse();
    }

    @Test
    void saveAndFlush_sameEmailTwice_isRejectedByUniqueConstraint() {
        userRepository.saveAndFlush(new User("ann@example.com", PASSWORD_HASH, Role.USER));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("ann@example.com", PASSWORD_HASH, Role.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
