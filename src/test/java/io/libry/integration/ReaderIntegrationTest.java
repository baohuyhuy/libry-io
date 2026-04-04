package io.libry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.librarian.TokenResponse;
import io.libry.repository.BorrowSlipRepository;
import io.libry.repository.LibrarianRepository;
import io.libry.repository.ReaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReaderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BorrowSlipRepository borrowSlipRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private LibrarianRepository librarianRepository;

    private String jwtToken;

    private final String futureDate = LocalDate
            .now()
            .plusYears(2)
            .toString();
    private final String pastDate = "1990-01-01";

    @BeforeEach
    void setUp() throws Exception {
        // delete children before parents to avoid FK constraint violations
        borrowSlipRepository.deleteAll();
        readerRepository.deleteAll();
        librarianRepository.deleteAll();

        // Register a librarian
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

        // Login and get JWT
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

    // --- POST /api/readers ---

    @Test
    void createReader_returns201_andCanBeRetrieved() throws Exception {
        String body = """
                {
                    "full_name": "Thomas Shelby",
                    "id_card_number": "84839281423",
                    "dob": "%s",
                    "expiry_date": "%s"
                }
                """.formatted(pastDate, futureDate);

        String location = mockMvc
                .perform(post("/api/readers")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/readers/")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        // Retrieve by returned location
        mockMvc
                .perform(get(location).header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Thomas Shelby"))
                .andExpect(jsonPath("$.id_card_number").value("84839281423"));
    }

    @Test
    void createReader_returns400_whenRequiredFieldsMissing() throws Exception {
        String body = """
                {
                    "full_name": "Thomas Shelby"
                }
                """;

        mockMvc
                .perform(post("/api/readers")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dob").exists())
                .andExpect(jsonPath("$.id_card_number").exists());
    }

    @Test
    void createReader_returns409_whenDuplicateIdCardNumber() throws Exception {
        String body = """
                {
                    "full_name": "Thomas Shelby",
                    "id_card_number": "84839281423",
                    "dob": "%s",
                    "expiry_date": "%s"
                }
                """.formatted(pastDate, futureDate);

        mockMvc.perform(post("/api/readers")
                .with(csrf())
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc
                .perform(post("/api/readers")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.id_card_number").exists());
    }

    // --- GET /api/readers ---

    @Test
    void getAllReaders_returns200WithList() throws Exception {
        createTestReader("Thomas Shelby", "11111111111");
        createTestReader("Arthur Shelby", "22222222222");

        mockMvc
                .perform(get("/api/readers").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- GET /api/readers/{id} ---

    @Test
    void findById_returns200_whenFound() throws Exception {
        long id = createTestReader("Thomas Shelby", "11111111111");

        mockMvc
                .perform(get("/api/readers/" + id).header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Thomas Shelby"));
    }

    @Test
    void findById_returns404_whenNotFound() throws Exception {
        mockMvc
                .perform(get("/api/readers/99999").header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/readers/{id} ---

    @Test
    void putReader_returns204_andUpdatesAllFields() throws Exception {
        long id = createTestReader("Thomas Shelby", "11111111111");

        String body = """
                {
                    "full_name": "Arthur Shelby",
                    "id_card_number": "22222222222",
                    "dob": "%s",
                    "expiry_date": "%s"
                }
                """.formatted(pastDate, futureDate);

        mockMvc
                .perform(put("/api/readers/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(get("/api/readers/" + id).header("Authorization", jwtToken))
                .andExpect(jsonPath("$.full_name").value("Arthur Shelby"))
                .andExpect(jsonPath("$.id_card_number").value("22222222222"));
    }

    // --- PATCH /api/readers/{id} ---

    @Test
    void patchReader_returns204_andUpdatesOnlyProvidedFields() throws Exception {
        long id = createTestReader("Thomas Shelby", "11111111111");

        String body = """
                {
                    "full_name": "Arthur Shelby"
                }
                """;

        mockMvc
                .perform(patch("/api/readers/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(get("/api/readers/" + id).header("Authorization", jwtToken))
                .andExpect(jsonPath("$.full_name").value("Arthur Shelby"))
                .andExpect(jsonPath("$.id_card_number").value("11111111111")); // unchanged
    }

    @Test
    void patchReader_returns400_whenDobIsInFuture() throws Exception {
        long id = createTestReader("Thomas Shelby", "11111111111");

        String body = """
                {
                    "dob": "%s"
                }
                """.formatted(LocalDate
                .now()
                .plusYears(1));

        mockMvc
                .perform(patch("/api/readers/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dob").exists());
    }

    // --- DELETE /api/readers/{id} ---

    @Test
    void deleteReader_returns204_andReaderNoLongerExists() throws Exception {
        long id = createTestReader("Thomas Shelby", "11111111111");

        mockMvc
                .perform(delete("/api/readers/" + id)
                        .with(csrf())
                        .header("Authorization", jwtToken))
                .andExpect(status().isNoContent());

        mockMvc
                .perform(get("/api/readers/" + id).header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReader_returns404_whenNotFound() throws Exception {
        mockMvc
                .perform(delete("/api/readers/99999")
                        .with(csrf())
                        .header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/readers/search ---

    @Test
    void searchByIdCardNumber_returns200_whenFound() throws Exception {
        createTestReader("Thomas Shelby", "11111111111");

        mockMvc
                .perform(get("/api/readers/search")
                        .param("id_card_number", "11111111111")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_card_number").value("11111111111"));
    }

    @Test
    void searchByFullName_returnsPartialMatches() throws Exception {
        createTestReader("Thomas Shelby", "11111111111");
        createTestReader("Thomas Edison", "22222222222");
        createTestReader("Arthur Shelby", "33333333333");

        mockMvc
                .perform(get("/api/readers/search")
                        .param("full_name", "thomas")
                        .header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void search_returns400_whenNoParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/readers/search").header("Authorization", jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns400_whenBothParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/readers/search")
                        .param("id_card_number", "11111111111")
                        .param("full_name", "thomas")
                        .header("Authorization", jwtToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anyEndpoint_returns401_whenNoToken() throws Exception {
        mockMvc
                .perform(get("/api/readers"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helper ---

    private long createTestReader(String fullName, String idCardNumber) throws Exception {
        String body = """
                {
                    "full_name": "%s",
                    "id_card_number": "%s",
                    "dob": "%s",
                    "expiry_date": "%s"
                }
                """.formatted(fullName, idCardNumber, pastDate, futureDate);

        String location = mockMvc
                .perform(post("/api/readers")
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
