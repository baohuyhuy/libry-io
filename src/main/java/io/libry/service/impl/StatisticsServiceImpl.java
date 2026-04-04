package io.libry.service.impl;

import io.libry.dto.statistics.BookStatisticResponse;
import io.libry.dto.statistics.ReaderStatisticsResponse;
import io.libry.repository.BookRepository;
import io.libry.repository.BorrowSlipRepository;
import io.libry.repository.ReaderRepository;
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
    private final ReaderRepository readerRepository;

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

    @Transactional(readOnly = true)
    @Override
    public ReaderStatisticsResponse getReaderStatistics() {
        long totalReaders = readerRepository.count();
        long totalActiveReaders = readerRepository.countActiveReaders();

        List<ReaderStatisticsResponse.GenderCount> byGender = readerRepository
                .countByGender()
                .stream()
                .map(row -> new ReaderStatisticsResponse.GenderCount((String) row[0], (long) row[1]))
                .toList();

        return new ReaderStatisticsResponse(totalReaders, totalActiveReaders, byGender);
    }
}
