package io.libry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record PutReaderRequest(@NotBlank String fullName,
                               @NotBlank String idCardNumber,
                               @Past LocalDate dob,
                               String gender,
                               @Email String email,
                               String address,
                               @Future LocalDate expiryDate) {
}
