package io.libry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.librarian.TokenResponse;
import io.libry.repository.BorrowSlipRepository;
import io.libry.repository.BookRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BorrowSlipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BorrowSlipRepository borrowSlipRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private LibrarianRepository librarianRepository;

    private String jwtToken;

    private final String futureExpiry = LocalDate.now().plusYears(2).toString();
    private final String pastDob = "1995-06-12";

    @BeforeEach
    void setUp() throws Exception {
        borrowSlipRepository.deleteAll();
        bookRepository.deleteAll();
        readerRepository.deleteAll();
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

        String loginResponse = mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = "Bearer " + objectMapper.readValue(loginResponse, TokenResponse.class).token();
    }

    // --- POST /api/borrow-slips ---

    @Test
    void createBorrowSlip_returns201_andLocationHeader() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);

        String body = """
                {
                    "reader_id": %d,
                    "book_ids": [%d]
                }
                """.formatted(readerId, bookId);

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/borrow-slips/")));
    }

    @Test
    void createBorrowSlip_decrementsBookQuantity() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);

        String body = """
                {
                    "reader_id": %d,
                    "book_ids": [%d]
                }
                """.formatted(readerId, bookId);

        mockMvc.perform(post("/api/borrow-slips")
                .with(csrf())
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc
                .perform(get("/api/books/" + bookId).header("Authorization", jwtToken))
                .andExpect(jsonPath("$.quantity").value(4));
    }

    @Test
    void createBorrowSlip_returns404_whenReaderNotFound() throws Exception {
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);

        String body = """
                {
                    "reader_id": 99999,
                    "book_ids": [%d]
                }
                """.formatted(bookId);

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createBorrowSlip_returns409_whenBookOutOfStock() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 0);

        String body = """
                {
                    "reader_id": %d,
                    "book_ids": [%d]
                }
                """.formatted(readerId, bookId);

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createBorrowSlip_returns400_whenDuplicateBookIds() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);

        String body = """
                {
                    "reader_id": %d,
                    "book_ids": [%d, %d]
                }
                """.formatted(readerId, bookId, bookId);

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createBorrowSlip_returns400_whenRequiredFieldsMissing() throws Exception {
        String body = """
                {
                    "book_ids": [1]
                }
                """;

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reader_id").exists());
    }

    @Test
    void createBorrowSlip_returns401_whenNoToken() throws Exception {
        String body = """
                {
                    "reader_id": 1,
                    "book_ids": [1]
                }
                """;

        mockMvc
                .perform(post("/api/borrow-slips")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/borrow-slips/{id} ---

    @Test
    void getBorrowSlipById_returns200_withCorrectFields() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));

        mockMvc
                .perform(get("/api/borrow-slips/" + slipId).header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slip_id").value(slipId))
                .andExpect(jsonPath("$.reader.reader_id").value(readerId))
                .andExpect(jsonPath("$.reader.full_name").value("James Walker"))
                .andExpect(jsonPath("$.books", hasSize(1)))
                .andExpect(jsonPath("$.books[0].title").value("To Kill a Mockingbird"))
                .andExpect(jsonPath("$.books[0].lost").value(false))
                .andExpect(jsonPath("$.actual_return_date").value(nullValue()));
    }

    @Test
    void getBorrowSlipById_returns404_whenNotFound() throws Exception {
        mockMvc
                .perform(get("/api/borrow-slips/99999").header("Authorization", jwtToken))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/borrow-slips ---

    @Test
    void getAllBorrowSlips_returns200WithList() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId1 = createTestBook("9780061120084", "To Kill a Mockingbird", 5);
        long bookId2 = createTestBook("9780452284234", "1984", 3);
        createTestBorrowSlip(readerId, List.of(bookId1));
        createTestBorrowSlip(readerId, List.of(bookId2));

        mockMvc
                .perform(get("/api/borrow-slips").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // --- PATCH /api/borrow-slips/{id}/return ---

    @Test
    void returnSlip_returns200_noFines_whenReturnedOnTime() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));

        String body = """
                {
                    "lost_book_ids": []
                }
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/" + slipId + "/return")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slip_id").value(slipId))
                .andExpect(jsonPath("$.overdue_days").value(0))
                .andExpect(jsonPath("$.overdue_fine").value(0))
                .andExpect(jsonPath("$.lost_book_fines").isEmpty())
                .andExpect(jsonPath("$.total_fine").value(0))
                .andExpect(jsonPath("$.actual_return_date").value(LocalDate.now().toString()));
    }

    @Test
    void returnSlip_incrementsBookQuantityBack() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));

        String body = """
                {
                    "lost_book_ids": []
                }
                """;

        mockMvc.perform(patch("/api/borrow-slips/" + slipId + "/return")
                .with(csrf())
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc
                .perform(get("/api/books/" + bookId).header("Authorization", jwtToken))
                .andExpect(jsonPath("$.quantity").value(5)); // restored
    }

    @Test
    void returnSlip_withLostBook_doesNotIncrementQuantity_andCalculatesFine() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));

        String body = """
                {
                    "lost_book_ids": [%d]
                }
                """.formatted(bookId);

        mockMvc
                .perform(patch("/api/borrow-slips/" + slipId + "/return")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lost_book_fines", hasSize(1)))
                .andExpect(jsonPath("$.lost_book_fines[0].title").value("To Kill a Mockingbird"))
                .andExpect(jsonPath("$.total_fine").value(25.98)); // 200% of 12.99

        mockMvc
                .perform(get("/api/books/" + bookId).header("Authorization", jwtToken))
                .andExpect(jsonPath("$.quantity").value(4)); // NOT restored — lost
    }

    @Test
    void returnSlip_returns422_whenAlreadyReturned() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));

        String body = """
                {
                    "lost_book_ids": []
                }
                """;

        mockMvc.perform(patch("/api/borrow-slips/" + slipId + "/return")
                .with(csrf())
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc
                .perform(patch("/api/borrow-slips/" + slipId + "/return")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returnSlip_returns404_whenSlipNotFound() throws Exception {
        String body = """
                {
                    "lost_book_ids": []
                }
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/99999/return")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnSlip_returns400_whenLostBookIdNotInSlip() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));

        String body = """
                {
                    "lost_book_ids": [99999]
                }
                """;

        mockMvc
                .perform(patch("/api/borrow-slips/" + slipId + "/return")
                        .with(csrf())
                        .header("Authorization", jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // --- Helpers ---

    private long createTestReader(String fullName, String idCardNumber) throws Exception {
        String body = """
                {
                    "full_name": "%s",
                    "id_card_number": "%s",
                    "dob": "%s",
                    "gender": "MALE",
                    "expiry_date": "%s"
                }
                """.formatted(fullName, idCardNumber, pastDob, futureExpiry);

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

    private long createTestBook(String isbn, String title, int quantity) throws Exception {
        String body = """
                {
                    "isbn": "%s",
                    "title": "%s",
                    "author": "Test Author",
                    "price": 12.99,
                    "quantity": %d
                }
                """.formatted(isbn, title, quantity);

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

    private long createTestBorrowSlip(long readerId, List<Long> bookIds) throws Exception {
        String bookIdList = bookIds.toString(); // e.g. [1, 2]

        String body = """
                {
                    "reader_id": %d,
                    "book_ids": %s
                }
                """.formatted(readerId, bookIdList);

        String location = mockMvc
                .perform(post("/api/borrow-slips")
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
