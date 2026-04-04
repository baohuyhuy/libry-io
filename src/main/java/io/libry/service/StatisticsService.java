package io.libry.service;

import io.libry.dto.statistics.BookStatisticResponse;
import io.libry.dto.statistics.ReaderStatisticsResponse;

public interface StatisticsService {
    BookStatisticResponse getBookStatistics();

    ReaderStatisticsResponse getReaderStatistics();
}
