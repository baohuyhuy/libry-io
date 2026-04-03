package io.libry.service.impl;

import io.libry.dto.book.BookRequest;
import io.libry.dto.book.BookResponse;
import io.libry.dto.book.PatchBookRequest;
import io.libry.entity.Book;
import io.libry.exception.ResourceNotFoundException;
import io.libry.repository.BookRepository;
import io.libry.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;

    @Override
    public List<BookResponse> getAllBooks() {
        return bookRepository
                .findAll()
                .stream()
                .map(BookResponse::from)
                .toList();
    }

    @Transactional
    @Override
    public BookResponse createBook(BookRequest bookRequest) {
        Book newBook = new Book();

        populateBookDetails(bookRequest, newBook);

        Book savedBook = bookRepository.save(newBook);
        return BookResponse.from(savedBook);
    }

    @Transactional
    @Override
    public void putBook(Long bookId, BookRequest request) {
        Book existingBook = bookRepository
                .findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " not found"));

        populateBookDetails(request, existingBook);

        bookRepository.save(existingBook);
    }

    @Transactional
    @Override
    public void deleteBook(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Book with id " + bookId + " not found");
        }
        bookRepository.deleteById(bookId);
    }

    @Override
    public BookResponse findByIsbn(String isbn) {
        return bookRepository
                .findByIsbn(isbn)
                .map(BookResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Book with ISBN " + isbn + " not found"));
    }

    @Override
    public List<BookResponse> findByTitle(String title) {
        return bookRepository
                .findByTitleContainingIgnoreCase(title)
                .stream()
                .map(BookResponse::from)
                .toList();
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
