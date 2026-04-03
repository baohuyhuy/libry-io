package io.libry.dto.slip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReturnSlipResponse(
        Long slipId,
        LocalDate borrowDate,
        LocalDate expectedReturnDate,
        LocalDate actualReturnDate,
        long overdueDays,
        BigDecimal overdueFine,
        List<LostBookFine> lostBookFines,
        BigDecimal totalFine
) {
    public record LostBookFine(Long bookId, String title, BigDecimal fine) {
    }
}
