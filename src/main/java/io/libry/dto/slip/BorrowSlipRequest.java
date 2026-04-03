package io.libry.dto.slip;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record BorrowSlipRequest(
        @NotNull @Positive Long readerId,
        @NotEmpty List<@NotNull @Positive Long> bookIds
) {
}
