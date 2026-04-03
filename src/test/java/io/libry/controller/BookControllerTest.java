package io.libry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.book.BookResponse;
import io.libry.dto.book.PatchBookRequest;
import io.libry.dto.book.BookRequest;
import io.libry.security.JwtAuthEntryPoint;
import io.libry.security.jwt.JwtService;
import io.libry.security.principal.UserDetailsServiceImpl;
import io.libry.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BookController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@WithMockUser
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl librarianDetailsService;

    @MockitoBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    private BookResponse bookResponse;

    @BeforeEach
    void setUp() {
        bookResponse = new BookResponse(
                1L,
                "9781593279509",
                "Eloquent JavaScript",
                "Marijn Haverbeke",
                "No Starch Press",
                2018,
                "Programming",
                new BigDecimal("39.99"),
                10,
                Instant.now()
        );
    }

    // --- GET /api/books ---

    @Test
    void getAllBooks_returns200WithList() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of(bookResponse));

        mockMvc
                .perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isbn").value("9781593279509"))
                .andExpect(jsonPath("$[0].title").value("Eloquent JavaScript"));
    }

    @Test
    void getAllBooks_returns200WithEmptyList() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of());

        mockMvc
                .perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/books/{id} ---

    @Test
    void getBookById_returns200_whenFound() throws Exception {
        when(bookService.findById(1L)).thenReturn(bookResponse);

        mockMvc
                .perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9781593279509"))
                .andExpect(jsonPath("$.publication_year").value(2018));
    }

    @Test
    void getBookById_returns404_whenNotFound() throws Exception {
        when(bookService.findById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc
                .perform(get("/api/books/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBookById_returns400_whenIdIsNotANumber() throws Exception {
        mockMvc
                .perform(get("/api/books/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- POST /api/books ---

    @Test
    void createBook_returns201_whenValid() throws Exception {
        when(bookService.createBook(any())).thenReturn(bookResponse);

        String body = """
                {
                    "isbn": "9781593279509",
                    "title": "Eloquent JavaScript",
                    "author": "Marijn Haverbeke",
                    "price": 39.99,
                    "quantity": 10
                }
                """;

        mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").exists())
                .andExpect(jsonPath("$.author").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void createBook_returns400_whenIsbnFormatIsInvalid() throws Exception {
        String body = """
                {
                    "isbn": "not-an-isbn",
                    "title": "Eloquent JavaScript",
                    "author": "Marijn Haverbeke",
                    "price": 39.99,
                    "quantity": 10
                }
                """;

        mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").exists());
    }

    @Test
    void createBook_returns400_whenPriceIsNegative() throws Exception {
        String body = """
                {
                    "isbn": "9781593279509",
                    "title": "Eloquent JavaScript",
                    "author": "Marijn Haverbeke",
                    "price": -1.00,
                    "quantity": 10
                }
                """;

        mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.price").exists());
    }

    @Test
    void createBook_returns400_whenQuantityIsNegative() throws Exception {
        String body = """
                {
                    "isbn": "9781593279509",
                    "title": "Eloquent JavaScript",
                    "author": "Marijn Haverbeke",
                    "price": 39.99,
                    "quantity": -1
                }
                """;

        mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void createBook_returns400_whenPublicationYearIsOutOfRange() throws Exception {
        String body = """
                {
                    "isbn": "9781593279509",
                    "title": "Eloquent JavaScript",
                    "author": "Marijn Haverbeke",
                    "price": 39.99,
                    "quantity": 10,
                    "publication_year": 500
                }
                """;

        mockMvc
                .perform(post("/api/books")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.publication_year").exists());
    }

    // --- PUT /api/books/{id} ---

    @Test
    void putBook_returns204_whenValid() throws Exception {
        doNothing().when(bookService).putBook(eq(1L), any(BookRequest.class));

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
                .perform(put("/api/books/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void putBook_returns404_whenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(bookService).putBook(eq(99L), any(BookRequest.class));

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
                .perform(put("/api/books/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/books/{id} ---

    @Test
    void patchBook_returns204_whenValid() throws Exception {
        doNothing().when(bookService).patchBook(eq(1L), any(PatchBookRequest.class));

        String body = """
                {
                    "title": "Eloquent JavaScript, 3rd Edition"
                }
                """;

        mockMvc
                .perform(patch("/api/books/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void patchBook_returns400_whenIsbnIsBlank() throws Exception {
        String body = """
                {
                    "isbn": ""
                }
                """;

        mockMvc
                .perform(patch("/api/books/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").exists());
    }

    @Test
    void patchBook_returns400_whenPublicationYearIsOutOfRange() throws Exception {
        String body = """
                {
                    "publication_year": 500
                }
                """;

        mockMvc
                .perform(patch("/api/books/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.publication_year").exists());
    }

    // --- DELETE /api/books/{id} ---

    @Test
    void deleteBook_returns204_whenFound() throws Exception {
        doNothing().when(bookService).deleteBook(1L);

        mockMvc
                .perform(delete("/api/books/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBook_returns404_whenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(bookService).deleteBook(99L);

        mockMvc
                .perform(delete("/api/books/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/books/search ---

    @Test
    void search_byIsbn_returns200_whenFound() throws Exception {
        when(bookService.findByIsbn("9781593279509")).thenReturn(bookResponse);

        mockMvc
                .perform(get("/api/books/search").param("isbn", "9781593279509"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9781593279509"));
    }

    @Test
    void search_byIsbn_returns404_whenNotFound() throws Exception {
        when(bookService.findByIsbn("0000000000000"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc
                .perform(get("/api/books/search").param("isbn", "0000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_byTitle_returns200WithMatches() throws Exception {
        when(bookService.findByTitle("eloquent")).thenReturn(List.of(bookResponse));

        mockMvc
                .perform(get("/api/books/search").param("title", "eloquent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Eloquent JavaScript"));
    }

    @Test
    void search_returns400_whenBothParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/books/search")
                        .param("isbn", "9781593279509")
                        .param("title", "eloquent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void search_returns400_whenNoParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/books/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
