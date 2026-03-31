package io.libry.dto.reader;

import io.libry.entity.Reader;

import java.time.Instant;
import java.time.LocalDate;

public record ReaderResponse(
        Long readerId,
        String fullName,
        String idCardNumber,
        LocalDate dob,
        String gender,
        String email,
        String address,
        LocalDate creationDate,
        LocalDate expiryDate,
        Instant createdAt
) {
    public static ReaderResponse from(Reader reader) {
        return new ReaderResponse(
                reader.getReaderId(),
                reader.getFullName(),
                reader.getIdCardNumber(),
                reader.getDob(),
                reader.getGender(),
                reader.getEmail(),
                reader.getAddress(),
                reader.getCreationDate(),
                reader.getExpiryDate(),
                reader.getCreatedAt()
        );
    }
}
