package io.libry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity(name = "librarians")
@Data
public class Librarian {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "librarian_id", nullable = false)
    private int librarianId;

    @NotNull
    @Column(name = "username", nullable = false)
    private String username;

    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
