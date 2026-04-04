package io.libry.controller;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.book.BookRequest;
import io.libry.dto.book.BookResponse;
import io.libry.dto.book.PatchBookRequest;
import io.libry.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<BookResponse>> getAllBooks(
            @PageableDefault(sort = "title") Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable("id") Long bookId) {
        return ResponseEntity.ok(bookService.findById(bookId));
    }

    @PostMapping
    public ResponseEntity<Void> createBook(@Valid @RequestBody BookRequest request) {
        BookResponse book = bookService.createBook(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(book.bookId())
                .toUri();

        return ResponseEntity
                .created(location)
                .build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> putBook(@PathVariable("id") Long bookId,
                                        @Valid @RequestBody BookRequest request) {
        bookService.putBook(bookId, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patchBook(@PathVariable("id") Long bookId,
                                          @Valid @RequestBody PatchBookRequest request) {
        bookService.patchBook(bookId, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable("id") Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity
                .noContent()
                .build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(value = "isbn", required = false) String isbn,
            @RequestParam(value = "title", required = false) String title,
            @PageableDefault(sort = "title") Pageable pageable) {
        if (isbn != null && title != null) {
            throw new IllegalArgumentException("Only one search parameter is allowed at a time");
        }
        if (isbn != null) {
            return ResponseEntity.ok(bookService.findByIsbn(isbn));
        }
        if (title != null) {
            return ResponseEntity.ok(bookService.findByTitle(title, pageable));
        }
        throw new IllegalArgumentException("Provide at least one search parameter: isbn or title");
    }
}
