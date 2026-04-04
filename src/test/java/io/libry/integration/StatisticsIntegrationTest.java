package io.libry.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.libry.dto.librarian.TokenResponse;
import io.libry.repository.BookRepository;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StatisticsIntegrationTest {

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

        String credentials = """
                {
                    "username": "admin",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials));

        String loginResponse = mockMvc
                .perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andReturn()
                .getResponse()
                .getContentAsString();

        jwtToken = "Bearer " + objectMapper.readValue(loginResponse, TokenResponse.class).token();
    }

    // --- GET /api/statistics/books ---

    @Test
    void getBookStatistics_returns200_withCorrectCounts() throws Exception {
        long bookId1 = createTestBook("9780061120084", "To Kill a Mockingbird", "Fiction", 5);
        long bookId2 = createTestBook("9780452284234", "1984", "Dystopian", 3);
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        createTestBorrowSlip(readerId, List.of(bookId1));

        mockMvc
                .perform(get("/api/statistics/books").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_titles").value(2))
                .andExpect(jsonPath("$.total_copies").value(7))  // 4 (decremented) + 3
                .andExpect(jsonPath("$.currently_borrowed").value(1))
                .andExpect(jsonPath("$.by_genre", hasSize(2)));
    }

    @Test
    void getBookStatistics_returns200_withZeroBorrowed_whenAllReturned() throws Exception {
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", "Fiction", 5);
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));
        returnSlip(slipId, List.of());

        mockMvc
                .perform(get("/api/statistics/books").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currently_borrowed").value(0));
    }

    @Test
    void getBookStatistics_returns401_whenNoToken() throws Exception {
        mockMvc
                .perform(get("/api/statistics/books"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/statistics/readers ---

    @Test
    void getReaderStatistics_returns200_withCorrectCounts() throws Exception {
        createTestReader("James Walker", "ID-TEST-001");
        createTestReaderWithGender("Emily Chen", "ID-TEST-002", "FEMALE");
        createTestReaderWithGender("Sophia Martinez", "ID-TEST-003", "FEMALE");

        mockMvc
                .perform(get("/api/statistics/readers").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_readers").value(3))
                .andExpect(jsonPath("$.active_readers").value(3))
                .andExpect(jsonPath("$.by_gender", hasSize(2)));
    }

    @Test
    void getReaderStatistics_returns401_whenNoToken() throws Exception {
        mockMvc
                .perform(get("/api/statistics/readers"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/statistics/readers/overdue ---

    @Test
    void getOverdueReaders_returns200_withOverdueSlip() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", "Fiction", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));

        // Force the slip to be overdue by backdating expectedReturnDate
        borrowSlipRepository.findById(slipId).ifPresent(slip -> {
            slip.setExpectedReturnDate(LocalDate.now().minusDays(3));
            borrowSlipRepository.save(slip);
        });

        mockMvc
                .perform(get("/api/statistics/readers/overdue").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reader_id").value(readerId))
                .andExpect(jsonPath("$[0].full_name").value("James Walker"))
                .andExpect(jsonPath("$[0].slip_id").value(slipId))
                .andExpect(jsonPath("$[0].overdue_days").value(3))
                .andExpect(jsonPath("$[0].books[0].title").value("To Kill a Mockingbird"));
    }

    @Test
    void getOverdueReaders_excludesReturnedSlips() throws Exception {
        long readerId = createTestReader("James Walker", "ID-TEST-001");
        long bookId = createTestBook("9780061120084", "To Kill a Mockingbird", "Fiction", 5);
        long slipId = createTestBorrowSlip(readerId, List.of(bookId));
        returnSlip(slipId, List.of());

        // Backdate expectedReturnDate to make it look overdue, but it's already returned
        borrowSlipRepository.findById(slipId).ifPresent(slip -> {
            slip.setExpectedReturnDate(LocalDate.now().minusDays(3));
            borrowSlipRepository.save(slip);
        });

        mockMvc
                .perform(get("/api/statistics/readers/overdue").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getOverdueReaders_returns200_withEmptyList_whenNoOverdueSlips() throws Exception {
        mockMvc
                .perform(get("/api/statistics/readers/overdue").header("Authorization", jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getOverdueReaders_returns401_whenNoToken() throws Exception {
        mockMvc
                .perform(get("/api/statistics/readers/overdue"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---

    private long createTestReader(String fullName, String idCardNumber) throws Exception {
        return createTestReaderWithGender(fullName, idCardNumber, "MALE");
    }

    private long createTestReaderWithGender(String fullName, String idCardNumber, String gender) throws Exception {
        String body = """
                {
                    "full_name": "%s",
                    "id_card_number": "%s",
                    "dob": "%s",
                    "gender": "%s",
                    "expiry_date": "%s"
                }
                """.formatted(fullName, idCardNumber, pastDob, gender, futureExpiry);

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

    private long createTestBook(String isbn, String title, String genre, int quantity) throws Exception {
        String body = """
                {
                    "isbn": "%s",
                    "title": "%s",
                    "author": "Test Author",
                    "genre": "%s",
                    "price": 12.99,
                    "quantity": %d
                }
                """.formatted(isbn, title, genre, quantity);

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
        String body = """
                {
                    "reader_id": %d,
                    "book_ids": %s
                }
                """.formatted(readerId, bookIds.toString());

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

    private void returnSlip(long slipId, List<Long> lostBookIds) throws Exception {
        String body = """
                {
                    "lost_book_ids": %s
                }
                """.formatted(lostBookIds.toString());

        mockMvc.perform(patch("/api/borrow-slips/" + slipId + "/return")
                .with(csrf())
                .header("Authorization", jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
