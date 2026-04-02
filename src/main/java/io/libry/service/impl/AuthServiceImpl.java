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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LibrarianRepository librarianRepo;

    private final AuthenticationManager authManager;

    private final JwtService jwtService;

    private final PasswordEncoder encoder;

    @Override
    public LibrarianResponse register(LibrarianRequest request) {
        if (librarianRepo.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }
        Librarian newLibrarian = new Librarian();
        newLibrarian.setUsername(request.username());
        newLibrarian.setPassword(encoder.encode(request.password()));

        return LibrarianResponse.from(librarianRepo.save(newLibrarian));
    }


    @Override
    public TokenResponse verify(LibrarianRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));
        return new TokenResponse(jwtService.generateToken(request.username()));
    }
}
