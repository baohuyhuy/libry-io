package io.libry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
@Getter
@Setter
public class BorrowSlipBookId implements Serializable {
    @Column(name = "slip_id")
    private Long slipId;

    @Column(name = "book_id")
    private Long bookId;
}
