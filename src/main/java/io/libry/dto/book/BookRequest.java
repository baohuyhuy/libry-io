package io.libry.dto.book;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BookRequest(
        @NotBlank
        @Pattern(regexp = "^(\\d{9}[\\dX]|\\d{13})$", message = "ISBN must be a valid ISBN-10 or ISBN-13")
        String isbn,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 255)
        String author,

        @Size(max = 255)
        String publisher,

        @Min(value = 1000, message = "Publication year must be at least 1000")
        @Max(value = 2100, message = "Publication year cannot exceed 2100")
        Integer publicationYear,

        @Size(max = 100)
        String genre,

        @NotNull
        @Positive
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull
        @Min(0)
        Integer quantity
) {
}
