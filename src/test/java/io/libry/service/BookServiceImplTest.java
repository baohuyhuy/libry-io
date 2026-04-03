package io.libry.service;

import io.libry.dto.book.BookRequest;
import io.libry.dto.book.BookResponse;
import io.libry.dto.book.PatchBookRequest;
import io.libry.entity.Book;
import io.libry.exception.ResourceNotFoundException;
import io.libry.repository.BookRepository;
import io.libry.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setBookId(1L);
        book.setIsbn("9781593279509");
        book.setTitle("Eloquent JavaScript");
        book.setAuthor("Marijn Haverbeke");
        book.setPublisher("No Starch Press");
        book.setPublicationYear(2018);
        book.setGenre("Programming");
        book.setPrice(new BigDecimal("39.99"));
        book.setQuantity(10);
    }

    // --- getAllBooks ---

    @Test
    void getAllBooks_returnsAllBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(book));

        List<BookResponse> result = bookService.getAllBooks();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isbn()).isEqualTo("9781593279509");
        assertThat(result.get(0).title()).isEqualTo("Eloquent JavaScript");
    }

    @Test
    void getAllBooks_returnsEmptyList_whenNoBooks() {
        when(bookRepository.findAll()).thenReturn(List.of());

        List<BookResponse> result = bookService.getAllBooks();

        assertThat(result).isEmpty();
    }

    // --- createBook ---

    @Test
    void createBook_savesAndReturnsBook() {
        BookRequest request = new BookRequest(
                "9781593279509", "Eloquent JavaScript", "Marijn Haverbeke",
                "No Starch Press", 2018, "Programming",
                new BigDecimal("39.99"), 10);

        when(bookRepository.save(any(Book.class))).thenReturn(book);

        BookResponse result = bookService.createBook(request);

        assertThat(result.isbn()).isEqualTo("9781593279509");
        assertThat(result.title()).isEqualTo("Eloquent JavaScript");
        verify(bookRepository).save(any(Book.class));
    }

    // --- findById ---

    @Test
    void findById_returnsBook_whenFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookResponse result = bookService.findById(1L);

        assertThat(result.bookId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Eloquent JavaScript");
    }

    @Test
    void findById_throws404_whenNotFound() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- findByIsbn ---

    @Test
    void findByIsbn_returnsBook_whenFound() {
        when(bookRepository.findByIsbn("9781593279509")).thenReturn(Optional.of(book));

        BookResponse result = bookService.findByIsbn("9781593279509");

        assertThat(result.isbn()).isEqualTo("9781593279509");
    }

    @Test
    void findByIsbn_throws404_whenNotFound() {
        when(bookRepository.findByIsbn("0000000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findByIsbn("0000000000000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("0000000000000");
    }

    // --- findByTitle ---

    @Test
    void findByTitle_returnsMatchingBooks() {
        when(bookRepository.findByTitleContainingIgnoreCase("eloquent")).thenReturn(List.of(book));

        List<BookResponse> result = bookService.findByTitle("eloquent");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Eloquent JavaScript");
    }

    @Test
    void findByTitle_returnsEmptyList_whenNoMatch() {
        when(bookRepository.findByTitleContainingIgnoreCase("xyz")).thenReturn(List.of());

        List<BookResponse> result = bookService.findByTitle("xyz");

        assertThat(result).isEmpty();
    }

    // --- putBook ---

    @Test
    void putBook_updatesAllFields() {
        BookRequest request = new BookRequest(
                "9780132350884", "Clean Code", "Robert C. Martin",
                "Prentice Hall", 2008, "Software Engineering",
                new BigDecimal("49.99"), 5);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.putBook(1L, request);

        assertThat(book.getIsbn()).isEqualTo("9780132350884");
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(book.getQuantity()).isEqualTo(5);
        verify(bookRepository).save(book);
    }

    @Test
    void putBook_throws404_whenNotFound() {
        BookRequest request = new BookRequest(
                "9780132350884", "Clean Code", "Robert C. Martin",
                null, null, null, new BigDecimal("49.99"), 5);

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.putBook(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(bookRepository, never()).save(any());
    }

    // --- patchBook ---

    @Test
    void patchBook_updatesOnlyProvidedFields() {
        PatchBookRequest request = new PatchBookRequest(
                null, "Eloquent JavaScript, 3rd Edition", null,
                null, null, null, null, null);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        bookService.patchBook(1L, request);

        assertThat(book.getTitle()).isEqualTo("Eloquent JavaScript, 3rd Edition");
        assertThat(book.getIsbn()).isEqualTo("9781593279509");    // unchanged
        assertThat(book.getAuthor()).isEqualTo("Marijn Haverbeke"); // unchanged
        verify(bookRepository).save(book);
    }

    @Test
    void patchBook_throws404_whenNotFound() {
        PatchBookRequest request = new PatchBookRequest(
                null, "New Title", null, null, null, null, null, null);

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.patchBook(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(bookRepository, never()).save(any());
    }

    // --- deleteBook ---

    @Test
    void deleteBook_deletesBook_whenFound() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.deleteBook(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBook_throws404_whenNotFound() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(bookRepository, never()).deleteById(any());
    }
}
