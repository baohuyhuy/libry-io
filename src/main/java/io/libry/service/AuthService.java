package io.libry.service;

import io.libry.dto.TokenResponse;
import io.libry.entity.Librarian;

public interface AuthService {
    Librarian register(Librarian librarian);

    TokenResponse verify(Librarian librarian);
}
