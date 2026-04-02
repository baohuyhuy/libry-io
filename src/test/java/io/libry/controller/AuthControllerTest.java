package io.libry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.librarian.LibrarianResponse;
import io.libry.dto.librarian.TokenResponse;
import io.libry.exception.ConflictException;
import io.libry.security.JwtAuthEntryPoint;
import io.libry.security.SecurityConfig;
import io.libry.security.jwt.JwtService;
import io.libry.security.principal.UserDetailsServiceImpl;
import io.libry.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl librarianDetailsService;

    @MockitoBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    // --- POST /api/auth/register ---

    @Test
    void register_returns201_whenValid() throws Exception {
        LibrarianResponse response = new LibrarianResponse(1L, "admin", Instant.now());
        when(authService.register(any())).thenReturn(response);

        mockMvc
                .perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "admin",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.librarian_id").value(1))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void register_returns409_whenUsernameAlreadyExists() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("Username already exists"));

        mockMvc
                .perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "admin",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_returns400_whenUsernameIsBlank() throws Exception {
        mockMvc
                .perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    void register_returns400_whenPasswordIsBlank() throws Exception {
        mockMvc
                .perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "admin",
                                    "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void register_returns400_whenFieldsAreMissing() throws Exception {
        mockMvc
                .perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    // --- POST /api/auth/login ---

    @Test
    void login_returns200WithToken_whenCredentialsAreValid() throws Exception {
        when(authService.verify(any())).thenReturn(new TokenResponse("mock.jwt.token"));

        mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "admin",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"));
    }

    @Test
    void login_returns401_whenCredentialsAreInvalid() throws Exception {
        when(authService.verify(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "admin",
                                    "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void login_returns400_whenFieldsAreMissing() throws Exception {
        mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    // --- POST /api/auth/logout ---

    @Test
    @WithMockUser
    void logout_returns204_whenAuthenticated() throws Exception {
        mockMvc
                .perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
