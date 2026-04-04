package io.libry.dto.statistics;

import java.time.LocalDate;
import java.util.List;

public record OverdueReaderResponse(
        Long readerId,
        String fullName,
        String idCardNumber,
        Long slipId,
        LocalDate borrowDate,
        LocalDate expectedReturnDate,
        long overdueDays,
        List<OverdueBook> books
) {
    public record OverdueBook(
            Long bookId,
            String isbn,
            String title
    ) {
    }
}
