package io.libry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.PatchReaderRequest;
import io.libry.dto.PutReaderRequest;
import io.libry.entity.Reader;
import io.libry.service.JWTService;
import io.libry.service.ReaderService;
import io.libry.service.impl.LibrarianDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReaderController.class)
@WithMockUser
class ReaderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReaderService readerService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private LibrarianDetailsServiceImpl librarianDetailsService;

    private Reader reader;

    @BeforeEach
    void setUp() {
        reader = new Reader();
        reader.setReaderId(1L);
        reader.setFullName("Thomas Shelby");
        reader.setIdCardNumber("84839281423");
        reader.setDob(LocalDate.of(1990, 1, 1));
        reader.setEmail("thomas@example.com");
        reader.setCreationDate(LocalDate.now());
        reader.setExpiryDate(LocalDate.now().plusYears(2));
    }

    // --- GET /api/readers/ ---

    @Test
    void getAllReaders_returns200WithList() throws Exception {
        when(readerService.getAllReaders()).thenReturn(List.of(reader));

        mockMvc.perform(get("/api/readers/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].full_name").value("Thomas Shelby"))
                .andExpect(jsonPath("$[0].id_card_number").value("84839281423"));
    }

    @Test
    void getAllReaders_returns200WithEmptyList() throws Exception {
        when(readerService.getAllReaders()).thenReturn(List.of());

        mockMvc.perform(get("/api/readers/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/readers/{id} ---

    @Test
    void findById_returns200_whenFound() throws Exception {
        when(readerService.findById(1L)).thenReturn(reader);

        mockMvc.perform(get("/api/readers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("Thomas Shelby"));
    }

    @Test
    void findById_returns404_whenNotFound() throws Exception {
        when(readerService.findById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/readers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returns400_whenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/readers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- POST /api/readers/ ---

    @Test
    void createReader_returns201_whenValid() throws Exception {
        when(readerService.createReader(any())).thenReturn(reader);

        String body = """
                {
                    "full_name": "Thomas Shelby",
                    "id_card_number": "84839281423",
                    "dob": "1990-01-01",
                    "expiry_date": "%s"
                }
                """.formatted(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/readers/")
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

        mockMvc.perform(post("/api/readers/")
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
                """.formatted(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/readers/")
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

        mockMvc.perform(post("/api/readers/")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dob").exists());
    }

    // --- PUT /api/readers/{id} ---

    @Test
    void putReader_returns204_whenValid() throws Exception {
        doNothing().when(readerService).putReader(eq(1L), any(PutReaderRequest.class));

        String body = """
                {
                    "full_name": "Arthur Shelby",
                    "id_card_number": "99999999999",
                    "dob": "1985-05-10",
                    "expiry_date": "%s"
                }
                """.formatted(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void putReader_returns404_whenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(readerService).putReader(eq(99L), any(PutReaderRequest.class));

        String body = """
                {
                    "full_name": "Arthur Shelby",
                    "id_card_number": "99999999999",
                    "dob": "1985-05-10",
                    "expiry_date": "%s"
                }
                """.formatted(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/readers/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/readers/{id} ---

    @Test
    void patchReader_returns204_whenValid() throws Exception {
        doNothing().when(readerService).patchReader(eq(1L), any(PatchReaderRequest.class));

        String body = """
                {
                    "full_name": "Arthur Shelby"
                }
                """;

        mockMvc.perform(patch("/api/readers/1")
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
                """.formatted(LocalDate.now().plusYears(1));

        mockMvc.perform(patch("/api/readers/1")
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

        mockMvc.perform(patch("/api/readers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expiry_date").exists());
    }

    // --- DELETE /api/readers/{id} ---

    @Test
    void deleteReader_returns204_whenFound() throws Exception {
        doNothing().when(readerService).deleteReader(1L);

        mockMvc.perform(delete("/api/readers/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReader_returns404_whenNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(readerService).deleteReader(99L);

        mockMvc.perform(delete("/api/readers/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/readers/search ---

    @Test
    void search_byIdCardNumber_returns200_whenFound() throws Exception {
        when(readerService.findByIdCardNumber("84839281423")).thenReturn(reader);

        mockMvc.perform(get("/api/readers/search").param("id_card_number", "84839281423"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_card_number").value("84839281423"));
    }

    @Test
    void search_byIdCardNumber_returns404_whenNotFound() throws Exception {
        when(readerService.findByIdCardNumber("000"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/readers/search").param("id_card_number", "000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_byFullName_returns200WithMatches() throws Exception {
        when(readerService.findByFullName("thomas")).thenReturn(List.of(reader));

        mockMvc.perform(get("/api/readers/search").param("full_name", "thomas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].full_name").value("Thomas Shelby"));
    }

    @Test
    void search_byFullName_returns200WithEmptyList_whenNoMatch() throws Exception {
        when(readerService.findByFullName("xyz")).thenReturn(List.of());

        mockMvc.perform(get("/api/readers/search").param("full_name", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_returns400_whenBothParamsGiven() throws Exception {
        mockMvc.perform(get("/api/readers/search")
                        .param("id_card_number", "84839281423")
                        .param("full_name", "thomas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void search_returns400_whenNoParamsGiven() throws Exception {
        mockMvc.perform(get("/api/readers/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
