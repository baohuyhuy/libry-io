package io.libry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatchReaderRequest(
        @Size(min = 1, message = "Full name cannot be blank") String fullName,
        @Size(min = 1, message = "ID card number cannot be blank") String idCardNumber,
        @Past LocalDate dob,
        String gender,
        @Email String email,
        String address,
        @Future LocalDate expiryDate
) {
}
