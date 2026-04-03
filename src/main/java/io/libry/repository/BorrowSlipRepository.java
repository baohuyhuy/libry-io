package io.libry.repository;

import io.libry.entity.BorrowSlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip, Long> {
    @Query("SELECT s FROM BorrowSlip s JOIN FETCH s.reader JOIN FETCH s.borrowSlipBooks sb JOIN FETCH sb.book WHERE s.slipId = :id")
    Optional<BorrowSlip> findByIdWithDetails(Long id);

    @Query("SELECT s FROM BorrowSlip s JOIN FETCH s.reader JOIN FETCH s.borrowSlipBooks sb JOIN FETCH sb.book")
    List<BorrowSlip> findAllWithDetails();
}
