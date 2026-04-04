package io.libry.repository;

import io.libry.entity.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long> {
    Optional<Reader> findByIdCardNumber(String idCardNumber);

    List<Reader> findByFullNameContainingIgnoreCase(String fullName);

    @Query("SELECT COUNT(r) FROM Reader r WHERE r.expiryDate >= CURRENT_DATE")
    long countActiveReaders();

    @Query("SELECT r.gender, COUNT(r) FROM Reader r GROUP BY r.gender")
    List<Object[]> countByGender();
}
