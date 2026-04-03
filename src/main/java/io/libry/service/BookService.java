package io.libry.service;

import io.libry.dto.book.BookRequest;
import io.libry.dto.book.BookResponse;
import io.libry.dto.book.PatchBookRequest;

import java.util.List;

public interface BookService {

    List<BookResponse> getAllBooks();

    BookResponse createBook(BookRequest bookRequest);

    void putBook(Long bookId, BookRequest request);

    void deleteBook(Long bookId);

    BookResponse findByIsbn(String isbn);

    List<BookResponse> findByTitle(String title);

    BookResponse findById(Long bookId);

    void patchBook(Long bookId, PatchBookRequest request);
}
