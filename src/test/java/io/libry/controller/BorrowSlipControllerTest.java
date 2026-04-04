package io.libry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.PaginatedResponse;
import io.libry.dto.slip.BorrowSlipResponse;
import io.libry.dto.slip.ReturnSlipResponse;
import io.libry.entity.BorrowSlip;
import io.libry.exception.ResourceNotFoundException;
import io.libry.exception.UnprocessableEntityException;
import io.libry.security.JwtAuthEntryPoint;
import io.libry.security.jwt.JwtService;
import io.libry.security.principal.UserDetailsServiceImpl;
import io.libry.service.BorrowSlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BorrowSlipController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@WithMockUser
class BorrowSlipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BorrowSlipService borrowSlipService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    private BorrowSlipResponse borrowSlipResponse;
    private ReturnSlipResponse returnSlipResponse;

    @BeforeEach
    void setUp() {
        BorrowSlipResponse.ReaderSummary reader = new BorrowSlipResponse.ReaderSummary(1L, "James Walker");
        BorrowSlipResponse.BorrowedBook borrowedBook = new BorrowSlipResponse.BorrowedBook(
                1L, "978-0-06-112008-4", "To Kill a Mockingbird", false);

        borrowSlipResponse = new BorrowSlipResponse(
                1L,
                reader,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                null,
                List.of(borrowedBook),
                Instant.now(),
                Instant.now()
        );

        ReturnSlipResponse.LostBookFine lostBookFine = new ReturnSlipResponse.LostBookFine(
                2L, "1984", new BigDecimal("19.98"));

        returnSlipResponse = new ReturnSlipResponse(
                1L,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(3),
                LocalDate.now(),
                3L,
                new BigDecimal("15000"),
                List.of(lostBookFine),
                new BigDecimal("15019.98")
        );
    }

    // --- POST /api/borrow-slips ---

    @Test
    void createBorrowSlip_returns201_whenValid() throws Exception {
        BorrowSlip slip = new BorrowSlip();
        slip.setSlipId(1L);
        when(borrowSlipService.createBorrowSlip(any())).thenReturn(slip);

        String body = """
                {
                    "reader_id": 1,
                    "book_ids": [1, 2]
                }
                """;

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/borrow-slips/1")));
    }

    @Test
    void createBorrowSlip_returns400_whenReaderIdMissing() throws Exception {
        String body = """
                {
                    "book_ids": [1, 2]
                }
                """;

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reader_id").exists());
    }

    @Test
    void createBorrowSlip_returns400_whenBookIdsEmpty() throws Exception {
        String body = """
                {
                    "reader_id": 1,
                    "book_ids": []
                }
                """;

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.book_ids").exists());
    }

    @Test
    void createBorrowSlip_returns400_whenBookIdIsNegative() throws Exception {
        String body = """
                {
                    "reader_id": 1,
                    "book_ids": [-1]
                }
                """;

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/borrow-slips/{id} ---

    @Test
    void getBorrowSlipById_returns200_whenFound() throws Exception {
        when(borrowSlipService.findById(1L)).thenReturn(borrowSlipResponse);

        mockMvc
                .perform(get("/api/borrow-slips/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slip_id").value(1))
                .andExpect(jsonPath("$.reader.reader_id").value(1))
                .andExpect(jsonPath("$.reader.full_name").value("James Walker"))
                .andExpect(jsonPath("$.books[0].title").value("To Kill a Mockingbird"))
                .andExpect(jsonPath("$.books[0].lost").value(false))
                .andExpect(jsonPath("$.actual_return_date").doesNotExist());
    }

    @Test
    void getBorrowSlipById_returns404_whenNotFound() throws Exception {
        when(borrowSlipService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Borrow slip with id 99 not found"));

        mockMvc
                .perform(get("/api/borrow-slips/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getBorrowSlipById_returns400_whenIdIsNotANumber() throws Exception {
        mockMvc
                .perform(get("/api/borrow-slips/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- GET /api/borrow-slips ---

    @Test
    void getAllBorrowSlips_returns200WithPagedResponse() throws Exception {
        PaginatedResponse<BorrowSlipResponse> pagedResponse = new PaginatedResponse<>(
                List.of(borrowSlipResponse), 0, 1, 1, 5, false, false);
        when(borrowSlipService.getAllBorrowSlips(any(Pageable.class))).thenReturn(pagedResponse);

        mockMvc
                .perform(get("/api/borrow-slips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slip_id").value(1))
                .andExpect(jsonPath("$.data[0].reader.full_name").value("James Walker"))
                .andExpect(jsonPath("$.total_items").value(1));
    }

    @Test
    void getAllBorrowSlips_returns200WithEmptyPage() throws Exception {
        PaginatedResponse<BorrowSlipResponse> emptyPage = new PaginatedResponse<>(
                List.of(), 0, 0, 0, 5, false, false);
        when(borrowSlipService.getAllBorrowSlips(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc
                .perform(get("/api/borrow-slips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.total_items").value(0));
    }

    // --- PATCH /api/borrow-slips/{id}/return ---

    @Test
    void returnBooks_returns200_whenValid() throws Exception {
        when(borrowSlipService.returnSlip(eq(1L), any())).thenReturn(returnSlipResponse);

        String body = """
                {
                    "lost_book_ids": [2]
                }
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/1/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slip_id").value(1))
                .andExpect(jsonPath("$.overdue_days").value(3))
                .andExpect(jsonPath("$.overdue_fine").value(15000))
                .andExpect(jsonPath("$.lost_book_fines[0].title").value("1984"))
                .andExpect(jsonPath("$.total_fine").value(15019.98));
    }

    @Test
    void returnBooks_returns200_withNoLostBooks() throws Exception {
        ReturnSlipResponse noFineResponse = new ReturnSlipResponse(
                1L,
                LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(4),
                LocalDate.now(),
                0L,
                BigDecimal.ZERO,
                List.of(),
                BigDecimal.ZERO
        );
        when(borrowSlipService.returnSlip(eq(1L), any())).thenReturn(noFineResponse);

        String body = """
                {
                    "lost_book_ids": []
                }
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/1/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue_days").value(0))
                .andExpect(jsonPath("$.total_fine").value(0))
                .andExpect(jsonPath("$.lost_book_fines").isEmpty());
    }

    @Test
    void returnBooks_returns404_whenSlipNotFound() throws Exception {
        when(borrowSlipService.returnSlip(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Borrow slip with id 99 not found"));

        String body = """
                {
                    "lost_book_ids": []
                }
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/99/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returnBooks_returns422_whenAlreadyReturned() throws Exception {
        when(borrowSlipService.returnSlip(eq(1L), any()))
                .thenThrow(new UnprocessableEntityException("Borrow slip has already been returned"));

        String body = """
                {
                    "lost_book_ids": []
                }
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/1/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returnBooks_returns400_whenLostBookIdsIsNull() throws Exception {
        String body = """
                {}
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/1/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.lost_book_ids").exists());
    }
}
