package io.libry.service.impl;

import io.libry.dto.librarian.LibrarianRequest;
import io.libry.dto.librarian.LibrarianResponse;
import io.libry.dto.librarian.TokenResponse;
import io.libry.entity.Librarian;
import io.libry.exception.ConflictException;
import io.libry.repository.LibrarianRepository;
import io.libry.security.jwt.JwtService;
import io.libry.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LibrarianRepository librarianRepo;

    private final AuthenticationManager authManager;

    private final JwtService jwtService;

    private final PasswordEncoder encoder;

    @Override
    public LibrarianResponse register(LibrarianRequest request) {
        log.info("Registering new librarian: username={}", request.username());
        if (librarianRepo.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }
        Librarian newLibrarian = new Librarian();
        newLibrarian.setUsername(request.username());
        newLibrarian.setPassword(encoder.encode(request.password()));

        LibrarianResponse response = LibrarianResponse.from(librarianRepo.save(newLibrarian));
        log.info("Librarian registered successfully: username={}", request.username());
        return response;
    }


    @Override
    public TokenResponse verify(LibrarianRequest request) {
        log.info("Login attempt: username={}", request.username());
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));
        log.info("Login successful: username={}", request.username());
        return new TokenResponse(jwtService.generateToken(request.username()));
    }
}
