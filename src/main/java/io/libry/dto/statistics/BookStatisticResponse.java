package io.libry.dto.statistics;

import java.util.List;

public record BookStatisticResponse(
        long totalTitles,
        long totalCopies,
        long currentlyBorrowed,
        List<GenreCount> byGenre
) {
    public record GenreCount(String genre, long titleCount, long copyCount) {
    }
}
