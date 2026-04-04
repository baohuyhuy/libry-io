package io.libry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.reader.PatchReaderRequest;
import io.libry.dto.reader.ReaderRequest;
import io.libry.dto.reader.ReaderResponse;
import io.libry.exception.ResourceNotFoundException;
import io.libry.security.JwtAuthEntryPoint;
import io.libry.security.jwt.JwtService;
import io.libry.security.principal.UserDetailsServiceImpl;
import io.libry.service.ReaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.libry.dto.PaginatedResponse;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ReaderController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@WithMockUser
class ReaderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReaderService readerService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl librarianDetailsService;

    @MockitoBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    private ReaderResponse readerResponse;

    @BeforeEach
    void setUp() {
        readerResponse = new ReaderResponse(
                1L,
                "Thomas Shelby",
                "84839281423",
                LocalDate.of(1990, 1, 1),
                null,
                "thomas@example.com",
                null,
                LocalDate.now(),
                LocalDate
                        .now()
                        .plusYears(2),
                null
        );
    }

    // --- GET /api/readers ---

    @Test
    void getAllReaders_returns200WithPagedResponse() throws Exception {
        PaginatedResponse<ReaderResponse> pagedResponse = new PaginatedResponse<>(
                List.of(readerResponse), 0, 1, 1, 10, false, false);
        when(readerService.getAllReaders(any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc
                .perform(get("/api/readers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].full_name").value("Thomas Shelby"))
                .andExpect(jsonPath("$.data[0].id_card_number").value("84839281423"))
                .andExpect(jsonPath("$.total_items").value(1))
                .andExpect(jsonPath("$.total_pages").value(1))
                .andExpect(jsonPath("$.current_page").value(0));
    }

    @Test
    void getAllReaders_returns200WithEmptyPage_whenNoReaders() throws Exception {
        PaginatedResponse<ReaderResponse> emptyPage = new PaginatedResponse<>(
                List.of(), 0, 0, 0, 10, false, false);
        when(readerService.getAllReaders(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc
                .perform(get("/api/readers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.total_items").value(0));
    }

    // --- GET /api/readers/{id} ---

    @Test
    void findById_returns200_whenFound() throws Exception {
        when(readerService.findById(1L)).thenReturn(readerResponse);

        mockMvc
                .perform(get("/api/readers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Thomas Shelby"));
    }

    @Test
    void findById_returns404_whenNotFound() throws Exception {
        when(readerService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Reader with id 99 not found"));

        mockMvc
                .perform(get("/api/readers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returns400_whenIdIsNotANumber() throws Exception {
        mockMvc
                .perform(get("/api/readers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- POST /api/readers ---

    @Test
    void createReader_returns201_whenValid() throws Exception {
        when(readerService.createReader(any())).thenReturn(readerResponse);

        String body = """
                {
                    "full_name": "Thomas Shelby",
                    "id_card_number": "84839281423",
                    "dob": "1990-01-01",
                    "expiry_date": "%s"
                }
                """.formatted(LocalDate
                .now()
                .plusYears(2));

        mockMvc
                .perform(post("/api/readers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dob").exists())
                .andExpect(jsonPath("$.id_card_number").exists());
    }

    @Test
    void createReader_returns400_whenInvalidEmail() throws Exception {
        String body = """
                {
                    "full_name": "Thomas Shelby",
                    "id_card_number": "84839281423",
                    "dob": "1990-01-01",
                    "email": "not-an-email",
                    "expiry_date": "%s"
                }
                """.formatted(LocalDate
                .now()
                .plusYears(2));

        mockMvc
                .perform(post("/api/readers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void createReader_returns400_whenInvalidDateFormat() throws Exception {
        String body = """
                {
                    "full_name": "Thomas Shelby",
                    "id_card_number": "84839281423",
                    "dob": "not-a-date"
                }
                """;

        mockMvc
                .perform(post("/api/readers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dob").exists());
    }

    // --- PUT /api/readers/{id} ---

    @Test
    void putReader_returns204_whenValid() throws Exception {
        doNothing()
                .when(readerService)
                .putReader(eq(1L), any(ReaderRequest.class));

        String body = """
                {
                    "full_name": "Arthur Shelby",
                    "id_card_number": "99999999999",
                    "dob": "1985-05-10",
                    "expiry_date": "%s"
                }
                """.formatted(LocalDate
                .now()
                .plusYears(3));

        mockMvc
                .perform(put("/api/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void putReader_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Reader with id 99 not found"))
                .when(readerService)
                .putReader(eq(99L), any(ReaderRequest.class));

        String body = """
                {
                    "full_name": "Arthur Shelby",
                    "id_card_number": "99999999999",
                    "dob": "1985-05-10",
                    "expiry_date": "%s"
                }
                """.formatted(LocalDate
                .now()
                .plusYears(3));

        mockMvc
                .perform(put("/api/readers/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/readers/{id} ---

    @Test
    void patchReader_returns204_whenValid() throws Exception {
        doNothing()
                .when(readerService)
                .patchReader(eq(1L), any(PatchReaderRequest.class));

        String body = """
                {
                    "full_name": "Arthur Shelby"
                }
                """;

        mockMvc
                .perform(patch("/api/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void patchReader_returns400_whenDobIsInFuture() throws Exception {
        String body = """
                {
                    "dob": "%s"
                }
                """.formatted(LocalDate
                .now()
                .plusYears(1));

        mockMvc
                .perform(patch("/api/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dob").exists());
    }

    @Test
    void patchReader_returns400_whenExpiryDateIsInPast() throws Exception {
        String body = """
                {
                    "expiry_date": "2000-01-01"
                }
                """;

        mockMvc
                .perform(patch("/api/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expiry_date").exists());
    }

    // --- DELETE /api/readers/{id} ---

    @Test
    void deleteReader_returns204_whenFound() throws Exception {
        doNothing()
                .when(readerService)
                .deleteReader(1L);

        mockMvc
                .perform(delete("/api/readers/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReader_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Reader with id 99 not found"))
                .when(readerService)
                .deleteReader(99L);

        mockMvc
                .perform(delete("/api/readers/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/readers/search ---

    @Test
    void search_byIdCardNumber_returns200_whenFound() throws Exception {
        when(readerService.findByIdCardNumber("84839281423")).thenReturn(readerResponse);

        mockMvc
                .perform(get("/api/readers/search").param("id_card_number", "84839281423"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_card_number").value("84839281423"));
    }

    @Test
    void search_byIdCardNumber_returns404_whenNotFound() throws Exception {
        when(readerService.findByIdCardNumber("000"))
                .thenThrow(new ResourceNotFoundException("Reader with id card number 000 not found"));

        mockMvc
                .perform(get("/api/readers/search").param("id_card_number", "000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_byFullName_returns200WithMatches() throws Exception {
        PaginatedResponse<ReaderResponse> pagedResponse = new PaginatedResponse<>(
                List.of(readerResponse), 0, 1, 1, 10, false, false);
        when(readerService.findByFullName(eq("thomas"), any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc
                .perform(get("/api/readers/search").param("full_name", "thomas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].full_name").value("Thomas Shelby"))
                .andExpect(jsonPath("$.total_items").value(1));
    }

    @Test
    void search_byFullName_returns200WithEmptyPage_whenNoMatch() throws Exception {
        PaginatedResponse<ReaderResponse> emptyPage = new PaginatedResponse<>(
                List.of(), 0, 0, 0, 10, false, false);
        when(readerService.findByFullName(eq("xyz"), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc
                .perform(get("/api/readers/search").param("full_name", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.total_items").value(0));
    }

    @Test
    void search_returns400_whenBothParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/readers/search")
                        .param("id_card_number", "84839281423")
                        .param("full_name", "thomas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void search_returns400_whenNoParamsGiven() throws Exception {
        mockMvc
                .perform(get("/api/readers/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
