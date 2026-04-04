package io.libry.service;

import io.libry.dto.statistics.BookStatisticResponse;

public interface StatisticsService {
    BookStatisticResponse getBookStatistics();
}
