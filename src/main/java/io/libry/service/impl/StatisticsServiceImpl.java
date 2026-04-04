package io.libry.service.impl;

import io.libry.dto.statistics.BookStatisticResponse;
import io.libry.repository.BookRepository;
import io.libry.repository.BorrowSlipRepository;
import io.libry.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final BookRepository bookRepository;
    private final BorrowSlipRepository borrowSlipRepository;

    @Transactional(readOnly = true)
    @Override
    public BookStatisticResponse getBookStatistics() {
        Long totalTitles = bookRepository.count();
        Long totalCopies = bookRepository.sumQuantity();
        Long currentlyBorrowed = borrowSlipRepository.countBorrowSlipBooks();

        List<BookStatisticResponse.GenreCount> byGenre = bookRepository
                .countByGenre()
                .stream()
                .map(row ->
                        new BookStatisticResponse.GenreCount(
                                (String) row[0],
                                (Long) row[1],
                                (Long) row[2]
                        ))
                .toList();

        return new BookStatisticResponse(totalTitles, totalCopies, currentlyBorrowed, byGenre);
    }
}
