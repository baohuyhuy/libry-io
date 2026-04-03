package io.libry.dto.book;

import io.libry.entity.Book;

import java.math.BigDecimal;
import java.time.Instant;

public record BookResponse(
        Long bookId,
        String isbn,
        String title,
        String author,
        String publisher,
        Integer publicationYear,
        String genre,
        BigDecimal price,
        Integer quantity,
        Instant createdAt
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getBookId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getPublicationYear(),
                book.getGenre(),
                book.getPrice(),
                book.getQuantity(),
                book.getCreatedAt()
        );
    }
}
