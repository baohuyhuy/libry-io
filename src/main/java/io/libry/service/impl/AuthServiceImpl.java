package io.libry.service.impl;


import io.libry.dto.TokenResponse;
import io.libry.entity.Librarian;
import io.libry.repository.LibrarianRepository;
import io.libry.service.AuthService;
import io.libry.service.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final LibrarianRepository librarianRepo;

    private final AuthenticationManager authManager;

    private final JWTService jwtService;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authManager, JWTService jwtService, LibrarianRepository librarianRepo) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.librarianRepo = librarianRepo;
    }

    @Override
    public Librarian register(Librarian librarian) {
        return librarianRepo.save(librarian);
    }


    @Override
    public TokenResponse verify(Librarian librarian) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(librarian.getUsername(), librarian.getPassword()));
        return new TokenResponse(jwtService.generateToken(librarian.getUsername()));
    }
}
