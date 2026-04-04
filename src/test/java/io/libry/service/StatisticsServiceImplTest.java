package io.libry.service;

import io.libry.dto.statistics.BookStatisticResponse;
import io.libry.dto.statistics.OverdueReaderResponse;
import io.libry.dto.statistics.ReaderStatisticsResponse;
import io.libry.entity.*;
import io.libry.repository.BookRepository;
import io.libry.repository.BorrowSlipRepository;
import io.libry.repository.ReaderRepository;
import io.libry.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowSlipRepository borrowSlipRepository;

    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    private Reader reader;
    private Book book1;
    private Book book2;

    @BeforeEach
    void setUp() {
        reader = new Reader();
        reader.setReaderId(1L);
        reader.setFullName("James Walker");
        reader.setIdCardNumber("ID-2098-00001");
        reader.setExpiryDate(LocalDate.now().plusMonths(48));

        book1 = new Book();
        book1.setBookId(1L);
        book1.setIsbn("978-0-06-112008-4");
        book1.setTitle("To Kill a Mockingbird");
        book1.setAuthor("Harper Lee");
        book1.setGenre("Fiction");
        book1.setPrice(new BigDecimal("12.99"));
        book1.setQuantity(5);

        book2 = new Book();
        book2.setBookId(2L);
        book2.setIsbn("978-0-452-28423-4");
        book2.setTitle("1984");
        book2.setAuthor("George Orwell");
        book2.setGenre("Dystopian");
        book2.setPrice(new BigDecimal("9.99"));
        book2.setQuantity(3);
    }

    // --- getBookStatistics ---

    @Test
    void getBookStatistics_returnsTotalsAndGenreBreakdown() {
        Object[] fictionRow = {"Fiction", 5L, 20L};
        Object[] scienceRow = {"Science", 3L, 10L};

        when(bookRepository.count()).thenReturn(8L);
        when(bookRepository.sumQuantity()).thenReturn(30L);
        when(borrowSlipRepository.countBorrowSlipBooks()).thenReturn(4L);
        when(bookRepository.countByGenre()).thenReturn(List.of(fictionRow, scienceRow));

        BookStatisticResponse result = statisticsService.getBookStatistics();

        assertThat(result.totalTitles()).isEqualTo(8L);
        assertThat(result.totalCopies()).isEqualTo(30L);
        assertThat(result.currentlyBorrowed()).isEqualTo(4L);
        assertThat(result.byGenre()).hasSize(2);
        assertThat(result.byGenre().get(0).genre()).isEqualTo("Fiction");
        assertThat(result.byGenre().get(0).titleCount()).isEqualTo(5L);
        assertThat(result.byGenre().get(0).copyCount()).isEqualTo(20L);
        assertThat(result.byGenre().get(1).genre()).isEqualTo("Science");
    }

    @Test
    void getBookStatistics_returnsZeroCurrentlyBorrowed_whenNoActiveSlips() {
        when(bookRepository.count()).thenReturn(5L);
        when(bookRepository.sumQuantity()).thenReturn(15L);
        when(borrowSlipRepository.countBorrowSlipBooks()).thenReturn(0L);
        when(bookRepository.countByGenre()).thenReturn(List.of());

        BookStatisticResponse result = statisticsService.getBookStatistics();

        assertThat(result.currentlyBorrowed()).isZero();
        assertThat(result.byGenre()).isEmpty();
    }

    // --- getReaderStatistics ---

    @Test
    void getReaderStatistics_returnsTotalsAndGenderBreakdown() {
        Object[] maleRow = {"MALE", 6L};
        Object[] femaleRow = {"FEMALE", 4L};

        when(readerRepository.count()).thenReturn(10L);
        when(readerRepository.countActiveReaders()).thenReturn(8L);
        when(readerRepository.countByGender()).thenReturn(List.of(maleRow, femaleRow));

        ReaderStatisticsResponse result = statisticsService.getReaderStatistics();

        assertThat(result.totalReaders()).isEqualTo(10L);
        assertThat(result.activeReaders()).isEqualTo(8L);
        assertThat(result.byGender()).hasSize(2);
        assertThat(result.byGender().get(0).gender()).isEqualTo("MALE");
        assertThat(result.byGender().get(0).count()).isEqualTo(6L);
        assertThat(result.byGender().get(1).gender()).isEqualTo("FEMALE");
        assertThat(result.byGender().get(1).count()).isEqualTo(4L);
    }

    // --- getOverdueReaders ---

    @Test
    void getOverdueReaders_returnsEmptyList_whenNoOverdueSlips() {
        when(borrowSlipRepository.findAllOverdue()).thenReturn(List.of());

        List<OverdueReaderResponse> result = statisticsService.getOverdueReaders();

        assertThat(result).isEmpty();
    }

    @Test
    void getOverdueReaders_calculatesOverdueDaysCorrectly() {
        LocalDate expectedReturnDate = LocalDate.now().minusDays(5);
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1), LocalDate.now().minusDays(12), expectedReturnDate);

        when(borrowSlipRepository.findAllOverdue()).thenReturn(List.of(slip));

        List<OverdueReaderResponse> result = statisticsService.getOverdueReaders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).readerId()).isEqualTo(1L);
        assertThat(result.get(0).fullName()).isEqualTo("James Walker");
        assertThat(result.get(0).slipId()).isEqualTo(1L);
        assertThat(result.get(0).overdueDays()).isEqualTo(5L);
        assertThat(result.get(0).books()).hasSize(1);
        assertThat(result.get(0).books().get(0).title()).isEqualTo("To Kill a Mockingbird");
    }

    @Test
    void getOverdueReaders_excludesLostBooks() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1, book2),
                LocalDate.now().minusDays(12), LocalDate.now().minusDays(5));

        // Mark book1 as lost
        slip.getBorrowSlipBooks().get(0).setLost(true);

        when(borrowSlipRepository.findAllOverdue()).thenReturn(List.of(slip));

        List<OverdueReaderResponse> result = statisticsService.getOverdueReaders();

        assertThat(result.get(0).books()).hasSize(1);
        assertThat(result.get(0).books().get(0).title()).isEqualTo("1984");
    }

    @Test
    void getOverdueReaders_returnsOneEntryPerSlip_whenReaderHasMultipleOverdueSlips() {
        BorrowSlip slip1 = buildSlip(1L, reader, List.of(book1), LocalDate.now().minusDays(12), LocalDate.now().minusDays(5));
        BorrowSlip slip2 = buildSlip(2L, reader, List.of(book2), LocalDate.now().minusDays(10), LocalDate.now().minusDays(3));

        when(borrowSlipRepository.findAllOverdue()).thenReturn(List.of(slip1, slip2));

        List<OverdueReaderResponse> result = statisticsService.getOverdueReaders();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).slipId()).isEqualTo(1L);
        assertThat(result.get(1).slipId()).isEqualTo(2L);
    }

    // --- Helper ---

    private BorrowSlip buildSlip(Long slipId, Reader reader, List<Book> books,
                                  LocalDate borrowDate, LocalDate expectedReturnDate) {
        BorrowSlip slip = new BorrowSlip();
        slip.setSlipId(slipId);
        slip.setReader(reader);
        slip.setBorrowDate(borrowDate);
        slip.setExpectedReturnDate(expectedReturnDate);
        slip.setActualReturnDate(null);

        List<BorrowSlipBook> slipBooks = new ArrayList<>();
        for (Book book : books) {
            BorrowSlipBookId id = new BorrowSlipBookId();
            id.setSlipId(slipId);
            id.setBookId(book.getBookId());

            BorrowSlipBook slipBook = new BorrowSlipBook();
            slipBook.setId(id);
            slipBook.setBorrowSlip(slip);
            slipBook.setBook(book);
            slipBooks.add(slipBook);
        }
        slip.getBorrowSlipBooks().addAll(slipBooks);

        return slip;
    }
}
