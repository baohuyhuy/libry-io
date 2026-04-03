package io.libry.dto.book;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PatchBookRequest(
        @Size(min = 1, message = "ISBN cannot be blank")
        @Pattern(regexp = "^(\\d{9}[\\dX]|\\d{13})$", message = "ISBN must be a valid ISBN-10 or ISBN-13")
        String isbn,

        @Size(min = 1, max = 255, message = "Title cannot be blank")
        String title,

        @Size(min = 1, max = 255, message = "Author cannot be blank")
        String author,

        @Size(max = 255)
        String publisher,

        @Min(value = 1000, message = "Publication year must be at least 1000")
        @Max(value = 2100, message = "Publication year cannot exceed 2100")
        Integer publicationYear,

        @Size(max = 100)
        String genre,

        @Positive
        @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
        BigDecimal price,

        @Min(0)
        Integer quantity
) {
}
