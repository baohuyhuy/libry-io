package io.libry.integration;

import io.libry.repository.LibrarianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibrarianRepository librarianRepository;

    @BeforeEach
    void setUp() {
        librarianRepository.deleteAll();
    }

    // --- POST /api/auth/register ---

    @Test
    void register_returns201_andPersistsLibrarian() throws Exception {
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
                .andExpect(jsonPath("$.librarian_id").isNumber())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void register_returns409_whenUsernameAlreadyExists() throws Exception {
        String body = """
                {
                    "username": "admin",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc
                .perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
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
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "username": "admin",
                            "password": "password123"
                        }
                        """));

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
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_returns401_whenPasswordIsWrong() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "username": "admin",
                            "password": "password123"
                        }
                        """));

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
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_whenUserDoesNotExist() throws Exception {
        mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "nobody",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
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
    void logout_returns204_whenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "username": "admin",
                            "password": "password123"
                        }
                        """));

        String tokenJson = mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "admin",
                                    "password": "password123"
                                }
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = tokenJson.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc
                .perform(post("/api/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_returns401_whenNotAuthenticated() throws Exception {
        mockMvc
                .perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
