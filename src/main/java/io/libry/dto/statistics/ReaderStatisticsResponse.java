package io.libry.dto.statistics;

import java.util.List;

public record ReaderStatisticsResponse(
        long totalReaders,
        long activeReaders,
        List<GenderCount> byGender
) {
    public record GenderCount(String gender, long count) {
    }
}
