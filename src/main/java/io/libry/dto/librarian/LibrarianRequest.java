package io.libry.dto.librarian;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibrarianRequest(
        @NotNull @Size(min = 1, message = "must not be empty") String username,
        @NotNull @Size(min = 1, message = "must not be empty") String password) {
}
