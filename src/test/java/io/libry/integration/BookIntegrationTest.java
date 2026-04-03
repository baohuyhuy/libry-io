package io.libry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.librarian.TokenResponse;
import io.libry.repository.BookRepository;
import io.libry.repository.LibrarianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibrarianRepository librarianRepository;

    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        bookRepository.deleteAll();
        librarianRepository.deleteAll();

        String registerBody = """
                {
                    "username": "admin",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));

        String loginBody = """
                {
                    "username": "admin",
                    "password": "password123"
                }
                """;

        String response = mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = "Bearer " + objectMapper
                .readValue(response, TokenResponse.class)
                .token();
    }

    // --- POST /api/books ---

    @Test
    void createBook_returns201_andCanBeRetrieved() throws Exception {
        String body = """
                {
                    "isbn": "9781593279509",
                    "title": "Eloquent JavaScript",
                    "author": "Marijn Haverbeke",
                    "publisher": "No Starch Press",
                    "publication_year": 2018,
                    "price": 39.99,
                    "quantity": 10
                }
                """;

        String location = mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/books/")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc
                .perform(get(location).header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9781593279509"))
                .andExpect(jsonPath("$.title").value("Eloquent JavaScript"))
                .andExpect(jsonPath("$.publication_year").value(2018));
    }

    @Test
    void createBook_returns400_whenRequiredFieldsMissing() throws Exception {
        String body = """
                {
                    "title": "Eloquent JavaScript"
                }
                """;

        mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").exists())
                .andExpect(jsonPath("$.author").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void createBook_returns409_whenDuplicateIsbn() throws Exception {
        String body = """
                {
                    "isbn": "9781593279509",
                    "title": "Eloquent JavaScript",
                    "author": "Marijn Haverbeke",
                    "price": 39.99,
                    "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/books")
                .with(csrf())
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isbn").exists());
    }

    // --- GET /api/books ---

    @Test
    void getAllBooks_returns200WithList() throws Exception {
        createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");
        createTestBook("9780132350884", "Clean Code", "Robert C. Martin");

        mockMvc
                .perform(get("/api/books").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- GET /api/books/{id} ---

    @Test
    void findById_returns200_whenFound() throws Exception {
        long id = createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");

        mockMvc
                .perform(get("/api/books/" + id).header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Eloquent JavaScript"));
    }

    @Test
    void findById_returns404_whenNotFound() throws Exception {
        mockMvc
                .perform(get("/api/books/99999").header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/books/{id} ---

    @Test
    void putBook_returns204_andUpdatesAllFields() throws Exception {
        long id = createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");

        String body = """
                {
                    "isbn": "9780132350884",
                    "title": "Clean Code",
                    "author": "Robert C. Martin",
                    "price": 49.99,
                    "quantity": 5
                }
                """;

        mockMvc
                .perform(put("/api/books/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(get("/api/books/" + id).header("Authorization", jwtToken))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    // --- PATCH /api/books/{id} ---

    @Test
    void patchBook_returns204_andUpdatesOnlyProvidedFields() throws Exception {
        long id = createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");

        String body = """
                {
                    "title": "Eloquent JavaScript, 3rd Edition"
                }
                """;

        mockMvc
                .perform(patch("/api/books/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(get("/api/books/" + id).header("Authorization", jwtToken))
                .andExpect(jsonPath("$.title").value("Eloquent JavaScript, 3rd Edition"))
                .andExpect(jsonPath("$.isbn").value("9781593279509"))    // unchanged
                .andExpect(jsonPath("$.author").value("Marijn Haverbeke")); // unchanged
    }

    @Test
    void patchBook_returns400_whenPublicationYearIsOutOfRange() throws Exception {
        long id = createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");

        String body = """
                {
                    "publication_year": 500
                }
                """;

        mockMvc
                .perform(patch("/api/books/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.publication_year").exists());
    }

    // --- DELETE /api/books/{id} ---

    @Test
    void deleteBook_returns204_andBookNoLongerExists() throws Exception {
        long id = createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");

        mockMvc
                .perform(delete("/api/books/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(get("/api/books/" + id).header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBook_returns404_whenNotFound() throws Exception {
        mockMvc
                .perform(delete("/api/books/99999")
                        .with(csrf())
                        .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/books/search ---

    @Test
    void searchByIsbn_returns200_whenFound() throws Exception {
        createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");

        mockMvc
                .perform(get("/api/books/search")
                        .param("isbn", "9781593279509")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9781593279509"));
    }

    @Test
    void searchByIsbn_returns404_whenNotFound() throws Exception {
        mockMvc
                .perform(get("/api/books/search")
                        .param("isbn", "9780000000000")
                        .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchByTitle_returnsPartialMatches() throws Exception {
        createTestBook("9781593279509", "Eloquent JavaScript", "Marijn Haverbeke");
        createTestBook("9780596517748", "JavaScript: The Good Parts", "Douglas Crockford");
        createTestBook("9780132350884", "Clean Code", "Robert C. Martin");

        mockMvc
                .perform(get("/api/books/search")
                        .param("title", "javascript")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void search_returns400_whenNoParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/books/search").header("Authorization", jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns400_whenBothParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/books/search")
                        .param("isbn", "9781593279509")
                        .param("title", "eloquent")
                        .header("Authorization", jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anyEndpoint_returns401_whenNoToken() throws Exception {
        mockMvc
                .perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helper ---

    private long createTestBook(String isbn, String title, String author) throws Exception {
        String body = """
                {
                    "isbn": "%s",
                    "title": "%s",
                    "author": "%s",
                    "price": 29.99,
                    "quantity": 5
                }
                """.formatted(isbn, title, author);

        String location = mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
