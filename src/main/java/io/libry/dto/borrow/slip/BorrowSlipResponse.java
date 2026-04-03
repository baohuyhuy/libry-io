package io.libry.dto.borrow.slip;

import io.libry.entity.Book;
import io.libry.entity.BorrowSlip;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BorrowSlipResponse(
        Long slipId,
        ReaderSummary reader,
        LocalDate borrowDate,
        LocalDate expectedReturnDate,
        LocalDate actualReturnDate,
        List<BorrowedBook> books,
        Instant createdAt,
        Instant updatedAt
) {
    public record ReaderSummary(Long readerId, String fullName) {
    }

    public record BorrowedBook(Long bookId, String isbn, String title,
                               boolean lost) {
    }

    public static BorrowSlipResponse from(BorrowSlip slip) {
        return new BorrowSlipResponse(
                slip.getSlipId(),
                new ReaderSummary(slip
                        .getReader()
                        .getReaderId(),
                        slip
                                .getReader()
                                .getFullName()),
                slip.getBorrowDate(),
                slip.getExpectedReturnDate(),
                slip.getActualReturnDate(),
                slip
                        .getBorrowSlipBooks()
                        .stream()
                        .map(sb -> {
                            Book book = sb.getBook();
                            return new BorrowedBook(
                                    book.getBookId(),
                                    book.getIsbn(),
                                    book.getTitle(),
                                    sb.isLost()
                            );
                        })
                        .toList(),
                slip.getCreatedAt(),
                slip.getUpdatedAt()
        );

    }
}
