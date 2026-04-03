package io.libry.dto.slip;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ReturnSlipRequest(
        @NotNull List<@NotNull @Positive Long> lostBookIds
) {
}
