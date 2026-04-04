package io.libry.service;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.book.BookRequest;
import io.libry.dto.book.BookResponse;
import io.libry.dto.book.PatchBookRequest;
import org.springframework.data.domain.Pageable;

public interface BookService {

    PaginatedResponse<BookResponse> getAllBooks(Pageable pageable);

    BookResponse createBook(BookRequest bookRequest);

    void putBook(Long bookId, BookRequest request);

    void deleteBook(Long bookId);

    BookResponse findByIsbn(String isbn);

    PaginatedResponse<BookResponse> findByTitle(String title, Pageable pageable);

    BookResponse findById(Long bookId);

    void patchBook(Long bookId, PatchBookRequest request);
}
