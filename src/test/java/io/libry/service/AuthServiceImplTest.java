package io.libry.service;

import io.libry.dto.librarian.LibrarianRequest;
import io.libry.dto.librarian.LibrarianResponse;
import io.libry.dto.librarian.TokenResponse;
import io.libry.entity.Librarian;
import io.libry.exception.ConflictException;
import io.libry.repository.LibrarianRepository;
import io.libry.security.jwt.JwtService;
import io.libry.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private LibrarianRepository librarianRepo;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private Librarian librarian;

    @BeforeEach
    void setUp() {
        librarian = new Librarian();
        librarian.setLibrarianId(1L);
        librarian.setUsername("admin");
        librarian.setPassword("$2a$12$hashedpassword");
        librarian.setCreatedAt(Instant.now());
    }

    // --- register ---

    @Test
    void register_savesAndReturnsLibrarianResponse_whenUsernameIsNew() {
        LibrarianRequest request = new LibrarianRequest("admin", "password123");

        when(librarianRepo.existsByUsername("admin")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("$2a$12$hashedpassword");
        when(librarianRepo.save(any(Librarian.class))).thenReturn(librarian);

        LibrarianResponse result = authService.register(request);

        assertThat(result.librarianId()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.createdAt()).isNotNull();

        verify(encoder).encode("password123");
        verify(librarianRepo).save(any(Librarian.class));
    }

    @Test
    void register_throwsConflictException_whenUsernameAlreadyExists() {
        LibrarianRequest request = new LibrarianRequest("admin", "password123");

        when(librarianRepo.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username already exists");

        verify(librarianRepo, never()).save(any());
    }

    // --- verify ---

    @Test
    void verify_returnsTokenResponse_whenCredentialsAreValid() {
        LibrarianRequest request = new LibrarianRequest("admin", "password123");

        when(jwtService.generateToken("admin")).thenReturn("mock.jwt.token");

        TokenResponse result = authService.verify(request);

        assertThat(result.token()).isEqualTo("mock.jwt.token");
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken("admin");
    }

    @Test
    void verify_throwsException_whenCredentialsAreInvalid() {
        LibrarianRequest request = new LibrarianRequest("admin", "wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.verify(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }
}
