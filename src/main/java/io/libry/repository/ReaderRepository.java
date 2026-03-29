package io.libry.repository;

import io.libry.entity.Reader;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long> {
    Optional<Reader> findByIdCardNumber(String idCardNumber);

    List<Reader> findByFullNameContainingIgnoreCase(String fullName);
}
