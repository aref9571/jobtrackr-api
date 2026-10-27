package io.github.aref9571.jobtrackr.service;

import io.github.aref9571.jobtrackr.dto.RegisterRequest;
import io.github.aref9571.jobtrackr.dto.RegisterResponse;
import io.github.aref9571.jobtrackr.exception.EmailAlreadyUsedException;
import io.github.aref9571.jobtrackr.model.Role;
import io.github.aref9571.jobtrackr.model.User;
import io.github.aref9571.jobtrackr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void register_newEmail_savesUserWithRoleUserAndReturnsIdAndEmail() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = service.register(new RegisterRequest("ann@example.com", PASSWORD));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.id()).isEqualTo(savedUser.getValue().getId());
        assertThat(response.email()).isEqualTo("ann@example.com");
    }

    @Test
    void register_newEmail_storesBcryptHashThatMatchesRawPassword() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(new RegisterRequest("ann@example.com", PASSWORD));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        String passwordHash = savedUser.getValue().getPasswordHash();
        assertThat(passwordHash).isNotEqualTo(PASSWORD);
        assertThat(passwordHash).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, passwordHash)).isTrue();
        assertThat(passwordEncoder.matches("wrong-password", passwordHash)).isFalse();
    }

    @Test
    void register_emailWithSpacesAndUpperCase_storesTrimmedLowerCaseEmail() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = service.register(new RegisterRequest("  Ann@Example.COM ", PASSWORD));

        assertThat(response.email()).isEqualTo("ann@example.com");
    }

    @Test
    void register_existingEmail_throwsExceptionWithoutSaving() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("ann@example.com", PASSWORD)))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasMessage("Email is already registered");
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void register_existingEmailInDifferentCase_throwsExceptionWithoutSaving() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("ANN@Example.com", PASSWORD)))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }
}
