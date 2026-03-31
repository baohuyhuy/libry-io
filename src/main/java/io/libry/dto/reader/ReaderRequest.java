package io.libry.dto.reader;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ReaderRequest(@NotBlank String fullName,
                            @NotBlank String idCardNumber,
                            @NotNull @Past LocalDate dob,
                            String gender,
                            @Email String email,
                            String address,
                            @Future LocalDate expiryDate) {
}
