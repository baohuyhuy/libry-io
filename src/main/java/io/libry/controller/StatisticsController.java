package io.libry.controller;

import io.libry.dto.statistics.BookStatisticResponse;
import io.libry.dto.statistics.OverdueReaderResponse;
import io.libry.dto.statistics.ReaderStatisticsResponse;
import io.libry.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statService;

    @GetMapping("/books")
    public BookStatisticResponse getBookStatistics() {
        return statService.getBookStatistics();
    }

    @GetMapping("/readers")
    public ReaderStatisticsResponse getReaderStatistics() {
        return statService.getReaderStatistics();
    }

    @GetMapping("/readers/overdue")
    public List<OverdueReaderResponse> getOverdueReaders() {
        return statService.getOverdueReaders();
    }
}
