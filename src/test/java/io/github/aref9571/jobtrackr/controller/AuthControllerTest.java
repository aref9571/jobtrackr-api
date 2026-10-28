package io.github.aref9571.jobtrackr.controller;

import io.github.aref9571.jobtrackr.config.SecurityConfig;
import io.github.aref9571.jobtrackr.dto.LoginRequest;
import io.github.aref9571.jobtrackr.dto.LoginResponse;
import io.github.aref9571.jobtrackr.dto.RegisterRequest;
import io.github.aref9571.jobtrackr.dto.RegisterResponse;
import io.github.aref9571.jobtrackr.exception.EmailAlreadyUsedException;
import io.github.aref9571.jobtrackr.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = UserDetailsService.class)
class AuthControllerTest {

    private static final UUID ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String PASSWORD = "correct-horse-battery";

    private final MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    AuthControllerTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void register_validRequest_returns201WithIdAndEmailOnly() throws Exception {
        RegisterRequest request = new RegisterRequest("ann@example.com", PASSWORD);
        when(authService.register(request)).thenReturn(new RegisterResponse(ID, "ann@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ann@example.com", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.*", hasSize(2)))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.email").value("ann@example.com"));

        verify(authService).register(request);
    }

    @Test
    void register_passwordWithMinimumAndMaximumLength_returns201() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(new RegisterResponse(ID, "ann@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ann@example.com", "a".repeat(8))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ann@example.com", "a".repeat(72))))
                .andExpect(status().isCreated());
    }

    @Test
    void register_emailAlreadyUsed_returns409ProblemDetail() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyUsedException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ANN@example.com", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Email is already registered"));
    }

    @Test
    void register_invalidEmail_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("not-an-email", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_emailWithSurroundingSpaces_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(" ann@example.com ", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_tooLongEmail_returns400WithFieldError() throws Exception {
        String email = "a".repeat(243) + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson(email, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void register_emptyBody_returns400WithBothFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("must not be blank"))
                .andExpect(jsonPath("$.errors.password").value("must not be blank"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_tooShortPassword_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ann@example.com", "a".repeat(7))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("size must be between 8 and 72"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_tooLongPassword_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ann@example.com", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("size must be between 8 and 72"));

        verifyNoInteractions(authService);
    }

    @Test
    void login_validCredentials_returns200WithBearerToken() throws Exception {
        LoginRequest request = new LoginRequest("ann@example.com", PASSWORD);
        when(authService.login(request)).thenReturn(new LoginResponse("header.payload.signature", "Bearer", 3600));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ann@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.accessToken").value("header.payload.signature"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authService).login(request);
    }

    @Test
    void login_badCredentials_returns401ProblemDetail() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("ann@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void login_emptyBody_returns400WithBothFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("must not be blank"))
                .andExpect(jsonPath("$.errors.password").value("must not be blank"));

        verifyNoInteractions(authService);
    }

    private static String credentialsJson(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }
}
