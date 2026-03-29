package io.libry.controller;

import io.libry.dto.TokenResponse;
import io.libry.entity.Librarian;
import io.libry.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Librarian register(@RequestBody Librarian librarian) {
        librarian.setPassword(encoder.encode(librarian.getPassword()));
        return authService.register(librarian);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody Librarian librarian) {
        return authService.verify(librarian);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
