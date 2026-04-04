package io.libry.repository;

import io.libry.entity.BorrowSlip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BorrowSlipRepository extends JpaRepository<BorrowSlip, Long> {
    @Query("SELECT s FROM BorrowSlip s JOIN FETCH s.reader JOIN FETCH s.borrowSlipBooks sb JOIN FETCH sb.book WHERE s.slipId = :id")
    Optional<BorrowSlip> findByIdWithDetails(Long id);

    @Query("SELECT s.slipId FROM BorrowSlip s")
    Page<Long> findPageOfIds(Pageable pageable);

    @Query("SELECT s FROM BorrowSlip s JOIN FETCH s.reader JOIN FETCH s.borrowSlipBooks sb JOIN FETCH sb.book WHERE s.slipId IN :ids")
    List<BorrowSlip> findAllWithDetailsByIds(List<Long> ids);

    @Query(
            "SELECT COUNT(sb) FROM BorrowSlip s " +
                    "JOIN s.borrowSlipBooks sb " +
                    "WHERE s.actualReturnDate IS NULL AND sb.lost = false"
    )
    Long countBorrowSlipBooks();

    @Query("SELECT s FROM BorrowSlip s " +
            "JOIN FETCH s.reader " +
            "JOIN FETCH s.borrowSlipBooks sb " +
            "JOIN FETCH sb.book " +
            "WHERE s.actualReturnDate IS NULL AND s.expectedReturnDate < CURRENT_DATE")
    List<BorrowSlip> findAllOverdue();
}
