package io.libry.service;

import io.libry.dto.statistics.BookStatisticResponse;
import io.libry.dto.statistics.OverdueReaderResponse;
import io.libry.dto.statistics.ReaderStatisticsResponse;

import java.util.List;

public interface StatisticsService {
    BookStatisticResponse getBookStatistics();

    ReaderStatisticsResponse getReaderStatistics();

    List<OverdueReaderResponse> getOverdueReaders();
}
