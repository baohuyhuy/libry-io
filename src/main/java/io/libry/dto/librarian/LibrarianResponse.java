package io.libry.dto.librarian;

import io.libry.entity.Librarian;

import java.time.Instant;

public record LibrarianResponse(
        Long librarianId,
        String username,
        Instant createdAt
) {
    public static LibrarianResponse from(Librarian librarian) {
        return new LibrarianResponse(
                librarian.getLibrarianId(),
                librarian.getUsername(),
                librarian.getCreatedAt()
        );
    }
}
