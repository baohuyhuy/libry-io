package io.libry.service.impl;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.book.BookRequest;
import io.libry.dto.book.BookResponse;
import io.libry.dto.book.PatchBookRequest;
import io.libry.entity.Book;
import io.libry.exception.ResourceNotFoundException;
import io.libry.repository.BookRepository;
import io.libry.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;

    @Override
    public PaginatedResponse<BookResponse> getAllBooks(Pageable pageable) {
        return PaginatedResponse.from(
                bookRepository.findAll(pageable).map(BookResponse::from)
        );
    }

    @Transactional
    @Override
    public BookResponse createBook(BookRequest bookRequest) {
        log.info("Creating book: isbn={}, title={}", bookRequest.isbn(), bookRequest.title());
        Book newBook = new Book();

        populateBookDetails(bookRequest, newBook);

        Book savedBook = bookRepository.save(newBook);
        log.info("Book created: bookId={}, isbn={}", savedBook.getBookId(), savedBook.getIsbn());
        return BookResponse.from(savedBook);
    }

    @Transactional
    @Override
    public void putBook(Long bookId, BookRequest request) {
        log.info("Replacing book: bookId={}", bookId);
        Book existingBook = bookRepository
                .findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " not found"));

        populateBookDetails(request, existingBook);

        bookRepository.save(existingBook);
        log.info("Book replaced: bookId={}", bookId);
    }

    @Transactional
    @Override
    public void deleteBook(Long bookId) {
        log.info("Deleting book: bookId={}", bookId);
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book with id " + bookId + " not found");
        }
        bookRepository.deleteById(bookId);
        log.info("Book deleted: bookId={}", bookId);
    }

    @Override
    public BookResponse findByIsbn(String isbn) {
        return bookRepository
                .findByIsbn(isbn)
                .map(BookResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN " + isbn + " not found"));
    }

    @Override
    public PaginatedResponse<BookResponse> findByTitle(String title, Pageable pageable) {
        return PaginatedResponse.from(
                bookRepository.findByTitleContainingIgnoreCase(title, pageable).map(BookResponse::from)
        );
    }

    @Override
    public BookResponse findById(Long bookId) {
        return bookRepository
                .findById(bookId)
                .map(BookResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " not found"));
    }

    @Transactional
    @Override
    public void patchBook(Long bookId, PatchBookRequest request) {
        log.info("Patching book: bookId={}", bookId);
        Book existingBook = bookRepository
                .findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " not found"));

        if (request.isbn() != null) {
            existingBook.setIsbn(request.isbn());
        }
        if (request.title() != null) {
            existingBook.setTitle(request.title());
        }
        if (request.author() != null) {
            existingBook.setAuthor(request.author());
        }
        if (request.publisher() != null) {
            existingBook.setPublisher(request.publisher());
        }
        if (request.publicationYear() != null) {
            existingBook.setPublicationYear(request.publicationYear());
        }
        if (request.genre() != null) {
            existingBook.setGenre(request.genre());
        }
        if (request.price() != null) {
            existingBook.setPrice(request.price());
        }
        if (request.quantity() != null) {
            existingBook.setQuantity(request.quantity());
        }

        bookRepository.save(existingBook);
        log.info("Book patched: bookId={}", bookId);
    }

    private void populateBookDetails(BookRequest request, Book existingBook) {
        existingBook.setIsbn(request.isbn());
        existingBook.setTitle(request.title());
        existingBook.setAuthor(request.author());
        existingBook.setPublisher(request.publisher());
        existingBook.setPublicationYear(request.publicationYear());
        existingBook.setGenre(request.genre());
        existingBook.setPrice(request.price());
        existingBook.setQuantity(request.quantity());
    }
}
