package io.libry.controller;

import io.libry.dto.statistics.BookStatisticResponse;
import io.libry.dto.statistics.OverdueReaderResponse;
import io.libry.dto.statistics.ReaderStatisticsResponse;
import io.libry.security.JwtAuthEntryPoint;
import io.libry.security.jwt.JwtService;
import io.libry.security.principal.UserDetailsServiceImpl;
import io.libry.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = StatisticsController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@WithMockUser
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    private BookStatisticResponse bookStatisticResponse;
    private ReaderStatisticsResponse readerStatisticsResponse;
    private OverdueReaderResponse overdueReaderResponse;

    @BeforeEach
    void setUp() {
        List<BookStatisticResponse.GenreCount> genreCounts = List.of(
                new BookStatisticResponse.GenreCount("Fiction", 5L, 20L),
                new BookStatisticResponse.GenreCount("Science", 3L, 10L)
        );
        bookStatisticResponse = new BookStatisticResponse(8L, 30L, 4L, genreCounts);

        List<ReaderStatisticsResponse.GenderCount> genderCounts = List.of(
                new ReaderStatisticsResponse.GenderCount("MALE", 6L),
                new ReaderStatisticsResponse.GenderCount("FEMALE", 4L)
        );
        readerStatisticsResponse = new ReaderStatisticsResponse(10L, 8L, genderCounts);

        List<OverdueReaderResponse.OverdueBook> overdueBooks = List.of(
                new OverdueReaderResponse.OverdueBook(1L, "978-0-06-112008-4", "To Kill a Mockingbird")
        );
        overdueReaderResponse = new OverdueReaderResponse(
                1L, "James Walker", "ID-2098-00001",
                1L,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(3),
                3L,
                overdueBooks
        );
    }

    // --- GET /api/statistics/books ---

    @Test
    void getBookStatistics_returns200_withCorrectFields() throws Exception {
        when(statisticsService.getBookStatistics()).thenReturn(bookStatisticResponse);

        mockMvc
                .perform(get("/api/statistics/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_titles").value(8))
                .andExpect(jsonPath("$.total_copies").value(30))
                .andExpect(jsonPath("$.currently_borrowed").value(4))
                .andExpect(jsonPath("$.by_genre[0].genre").value("Fiction"))
                .andExpect(jsonPath("$.by_genre[0].title_count").value(5))
                .andExpect(jsonPath("$.by_genre[0].copy_count").value(20))
                .andExpect(jsonPath("$.by_genre[1].genre").value("Science"));
    }

    // --- GET /api/statistics/readers ---

    @Test
    void getReaderStatistics_returns200_withCorrectFields() throws Exception {
        when(statisticsService.getReaderStatistics()).thenReturn(readerStatisticsResponse);

        mockMvc
                .perform(get("/api/statistics/readers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_readers").value(10))
                .andExpect(jsonPath("$.active_readers").value(8))
                .andExpect(jsonPath("$.by_gender[0].gender").value("MALE"))
                .andExpect(jsonPath("$.by_gender[0].count").value(6))
                .andExpect(jsonPath("$.by_gender[1].gender").value("FEMALE"))
                .andExpect(jsonPath("$.by_gender[1].count").value(4));
    }

    // --- GET /api/statistics/readers/overdue ---

    @Test
    void getOverdueReaders_returns200_withList() throws Exception {
        when(statisticsService.getOverdueReaders()).thenReturn(List.of(overdueReaderResponse));

        mockMvc
                .perform(get("/api/statistics/readers/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reader_id").value(1))
                .andExpect(jsonPath("$[0].full_name").value("James Walker"))
                .andExpect(jsonPath("$[0].id_card_number").value("ID-2098-00001"))
                .andExpect(jsonPath("$[0].slip_id").value(1))
                .andExpect(jsonPath("$[0].overdue_days").value(3))
                .andExpect(jsonPath("$[0].books[0].title").value("To Kill a Mockingbird"));
    }

    @Test
    void getOverdueReaders_returns200_withEmptyList() throws Exception {
        when(statisticsService.getOverdueReaders()).thenReturn(List.of());

        mockMvc
                .perform(get("/api/statistics/readers/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
