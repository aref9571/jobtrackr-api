package io.github.aref9571.jobtrackr.service;

import io.github.aref9571.jobtrackr.dto.RegisterRequest;
import io.github.aref9571.jobtrackr.dto.RegisterResponse;
import io.github.aref9571.jobtrackr.exception.EmailAlreadyUsedException;
import io.github.aref9571.jobtrackr.model.Role;
import io.github.aref9571.jobtrackr.model.User;
import io.github.aref9571.jobtrackr.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException("Email is already registered");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), Role.USER);
        return RegisterResponse.from(userRepository.saveAndFlush(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
