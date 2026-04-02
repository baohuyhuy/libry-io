package io.libry.service;

import io.libry.dto.librarian.LibrarianRequest;
import io.libry.dto.librarian.LibrarianResponse;
import io.libry.dto.librarian.TokenResponse;

public interface AuthService {
    LibrarianResponse register(LibrarianRequest request);

    TokenResponse verify(LibrarianRequest request);
}
