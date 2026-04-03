package io.libry.service;

import io.libry.dto.slip.BorrowSlipRequest;
import io.libry.dto.slip.BorrowSlipResponse;
import io.libry.dto.slip.ReturnSlipRequest;
import io.libry.dto.slip.ReturnSlipResponse;
import io.libry.entity.*;
import io.libry.exception.ConflictException;
import io.libry.exception.ResourceNotFoundException;
import io.libry.exception.UnprocessableEntityException;
import io.libry.repository.BookRepository;
import io.libry.repository.BorrowSlipRepository;
import io.libry.repository.ReaderRepository;
import io.libry.service.impl.BorrowSlipServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowSlipServiceImplTest {

    @Mock
    private ReaderRepository readerRepository;

    @Mock
    private BorrowSlipRepository borrowSlipRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BorrowSlipServiceImpl borrowSlipService;

    private Reader reader;
    private Book book1;
    private Book book2;

    @BeforeEach
    void setUp() {
        reader = new Reader();
        reader.setReaderId(1L);
        reader.setFullName("James Walker");
        reader.setIdCardNumber("ID-2098-00001");
        reader.setDob(LocalDate.of(1995, 6, 12));
        reader.setGender("MALE");
        reader.setEmail("james.walker@gmail.com");
        reader.setAddress("42 Baker Street, London");
        reader.setCreationDate(LocalDate.now());
        reader.setExpiryDate(LocalDate.now().plusMonths(48));

        book1 = new Book();
        book1.setBookId(1L);
        book1.setIsbn("978-0-06-112008-4");
        book1.setTitle("To Kill a Mockingbird");
        book1.setAuthor("Harper Lee");
        book1.setPrice(new BigDecimal("12.99"));
        book1.setQuantity(5);

        book2 = new Book();
        book2.setBookId(2L);
        book2.setIsbn("978-0-452-28423-4");
        book2.setTitle("1984");
        book2.setAuthor("George Orwell");
        book2.setPrice(new BigDecimal("9.99"));
        book2.setQuantity(3);
    }

    // --- createBorrowSlip ---

    @Test
    void createBorrowSlip_savesSlipAndDecrementsQuantity() {
        BorrowSlipRequest request = new BorrowSlipRequest(1L, List.of(1L, 2L));

        BorrowSlip savedSlip = new BorrowSlip();
        savedSlip.setSlipId(1L);

        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book2));
        when(borrowSlipRepository.save(any())).thenReturn(savedSlip);

        BorrowSlip result = borrowSlipService.createBorrowSlip(request);

        assertThat(result.getSlipId()).isEqualTo(1L);
        assertThat(book1.getQuantity()).isEqualTo(4);
        assertThat(book2.getQuantity()).isEqualTo(2);
        verify(borrowSlipRepository).save(any(BorrowSlip.class));
    }

    @Test
    void createBorrowSlip_throws404_whenReaderNotFound() {
        BorrowSlipRequest request = new BorrowSlipRequest(99L, List.of(1L));

        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowSlipService.createBorrowSlip(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(borrowSlipRepository, never()).save(any());
    }

    @Test
    void createBorrowSlip_throws422_whenReaderCardExpired() {
        reader.setExpiryDate(LocalDate.now().minusDays(1));
        BorrowSlipRequest request = new BorrowSlipRequest(1L, List.of(1L));

        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));

        assertThatThrownBy(() -> borrowSlipService.createBorrowSlip(request))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("expired");
        verify(borrowSlipRepository, never()).save(any());
    }

    @Test
    void createBorrowSlip_throwsIllegalArgument_whenDuplicateBookIds() {
        BorrowSlipRequest request = new BorrowSlipRequest(1L, List.of(1L, 1L));

        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));

        assertThatThrownBy(() -> borrowSlipService.createBorrowSlip(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
        verify(borrowSlipRepository, never()).save(any());
    }

    @Test
    void createBorrowSlip_throws404_whenBookNotFound() {
        BorrowSlipRequest request = new BorrowSlipRequest(1L, List.of(99L));

        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowSlipService.createBorrowSlip(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(borrowSlipRepository, never()).save(any());
    }

    @Test
    void createBorrowSlip_throwsConflict_whenBookOutOfStock() {
        book1.setQuantity(0);
        BorrowSlipRequest request = new BorrowSlipRequest(1L, List.of(1L));

        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));

        assertThatThrownBy(() -> borrowSlipService.createBorrowSlip(request))
                .isInstanceOf(ConflictException.class);
        verify(borrowSlipRepository, never()).save(any());
    }

    // --- findById ---

    @Test
    void findById_returnsResponse_whenFound() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1), LocalDate.now(), LocalDate.now().plusDays(7), null);

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        BorrowSlipResponse result = borrowSlipService.findById(1L);

        assertThat(result.slipId()).isEqualTo(1L);
        assertThat(result.reader().readerId()).isEqualTo(1L);
        assertThat(result.reader().fullName()).isEqualTo("James Walker");
        assertThat(result.books()).hasSize(1);
        assertThat(result.actualReturnDate()).isNull();
    }

    @Test
    void findById_throws404_whenNotFound() {
        when(borrowSlipRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowSlipService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getAllBorrowSlips ---

    @Test
    void getAllBorrowSlips_returnsList() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1), LocalDate.now(), LocalDate.now().plusDays(7), null);

        when(borrowSlipRepository.findAllWithDetails()).thenReturn(List.of(slip));

        List<BorrowSlipResponse> result = borrowSlipService.getAllBorrowSlips();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllBorrowSlips_returnsEmptyList() {
        when(borrowSlipRepository.findAllWithDetails()).thenReturn(List.of());

        List<BorrowSlipResponse> result = borrowSlipService.getAllBorrowSlips();

        assertThat(result).isEmpty();
    }

    // --- returnSlip ---

    @Test
    void returnSlip_success_noLostBooks_incrementsQuantity() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1, book2),
                LocalDate.now().minusDays(3), LocalDate.now().plusDays(4), null);

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        ReturnSlipResponse result = borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of()));

        assertThat(result.overdueDays()).isZero();
        assertThat(result.overdueFine()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.lostBookFines()).isEmpty();
        assertThat(result.totalFine()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(book1.getQuantity()).isEqualTo(6);
        assertThat(book2.getQuantity()).isEqualTo(4);
        assertThat(slip.getActualReturnDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void returnSlip_success_withLostBook_doesNotIncrementQuantity_andCalculatesFine() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1, book2),
                LocalDate.now().minusDays(3), LocalDate.now().plusDays(4), null);

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        ReturnSlipResponse result = borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of(1L)));

        // book1 lost: 200% of 12.99 = 25.98
        assertThat(result.lostBookFines()).hasSize(1);
        assertThat(result.lostBookFines().get(0).bookId()).isEqualTo(1L);
        assertThat(result.lostBookFines().get(0).fine()).isEqualByComparingTo(new BigDecimal("25.98"));
        assertThat(result.totalFine()).isEqualByComparingTo(new BigDecimal("25.98"));
        assertThat(book1.getQuantity()).isEqualTo(5); // NOT incremented — lost
        assertThat(book2.getQuantity()).isEqualTo(4); // incremented — returned
    }

    @Test
    void returnSlip_success_overdue_calculatesOverdueFine() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1),
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), null);

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        ReturnSlipResponse result = borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of()));

        assertThat(result.overdueDays()).isEqualTo(3);
        assertThat(result.overdueFine()).isEqualByComparingTo(new BigDecimal("15000")); // 3 × 5000
        assertThat(result.totalFine()).isEqualByComparingTo(new BigDecimal("15000"));
    }

    @Test
    void returnSlip_success_overdueAndLostBook_sumsBothFines() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1),
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(2), null);

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        ReturnSlipResponse result = borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of(1L)));

        // overdue: 2 days × 5000 = 10000, lost: 200% of 12.99 = 25.98
        assertThat(result.overdueFine()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(result.lostBookFines().get(0).fine()).isEqualByComparingTo(new BigDecimal("25.98"));
        assertThat(result.totalFine()).isEqualByComparingTo(new BigDecimal("10025.98"));
    }

    @Test
    void returnSlip_success_returnedEarly_noOverdueFine() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1),
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(5), null);

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        ReturnSlipResponse result = borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of()));

        assertThat(result.overdueDays()).isZero();
        assertThat(result.overdueFine()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void returnSlip_throws404_whenSlipNotFound() {
        when(borrowSlipRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowSlipService.returnSlip(99L, new ReturnSlipRequest(List.of())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void returnSlip_throws422_whenAlreadyReturned() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1),
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        assertThatThrownBy(() -> borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of())))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("already been returned");
    }

    @Test
    void returnSlip_throwsIllegalArgument_whenDuplicateLostBookIds() {
        assertThatThrownBy(() -> borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of(1L, 1L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void returnSlip_throwsIllegalArgument_whenLostBookNotInSlip() {
        BorrowSlip slip = buildSlip(1L, reader, List.of(book1),
                LocalDate.now().minusDays(3), LocalDate.now().plusDays(4), null);

        when(borrowSlipRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(slip));

        assertThatThrownBy(() -> borrowSlipService.returnSlip(1L, new ReturnSlipRequest(List.of(99L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    // --- Helper ---

    private BorrowSlip buildSlip(Long slipId, Reader reader, List<Book> books,
                                  LocalDate borrowDate, LocalDate expectedReturnDate,
                                  LocalDate actualReturnDate) {
        BorrowSlip slip = new BorrowSlip();
        slip.setSlipId(slipId);
        slip.setReader(reader);
        slip.setBorrowDate(borrowDate);
        slip.setExpectedReturnDate(expectedReturnDate);
        slip.setActualReturnDate(actualReturnDate);

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
