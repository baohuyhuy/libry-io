package io.libry.repository;

import io.libry.entity.Librarian;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibrarianRepository extends JpaRepository<Librarian, Long> {
    Librarian findByUsername(String username);
}
