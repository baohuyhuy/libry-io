package io.libry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "borrow_slip_books")
@Getter
@Setter
public class BorrowSlipBook {
    @EmbeddedId
    private BorrowSlipBookId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("slipId")
    @JoinColumn(name = "slip_id", nullable = false)
    private BorrowSlip borrowSlip;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bookId")
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "is_lost", nullable = false)
    private boolean lost = false;
}
