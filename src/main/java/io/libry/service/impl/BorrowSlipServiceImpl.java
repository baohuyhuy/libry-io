package io.libry.service.impl;

import io.libry.dto.borrow.slip.BorrowSlipRequest;
import io.libry.dto.borrow.slip.BorrowSlipResponse;
import io.libry.entity.*;
import io.libry.exception.ConflictException;
import io.libry.exception.ResourceNotFoundException;
import io.libry.exception.UnprocessableEntityException;
import io.libry.repository.BookRepository;
import io.libry.repository.BorrowSlipRepository;
import io.libry.repository.ReaderRepository;
import io.libry.service.BorrowSlipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowSlipServiceImpl implements BorrowSlipService {
    private final ReaderRepository readerRepository;

    private final BorrowSlipRepository borrowSlipRepository;
    private final BookRepository bookRepository;

    @Transactional
    @Override
    public BorrowSlip createBorrowSlip(BorrowSlipRequest request) {
        log.info("Creating borrow slip: readerId={}, bookIds={}", request.readerId(), request.bookIds());

        // Validate reader exists
        Reader reader = readerRepository
                .findById(request.readerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reader with id " + request.readerId() + " not found"));

        // Validate reader card is not expired
        if (reader
                .getExpiryDate()
                .isBefore(LocalDate.now())) {
            throw new UnprocessableEntityException("Reader card is expired");
        }

        // Validate book IDs are unique within the request
        List<Long> bookIds = request.bookIds();
        if (bookIds.size() != new HashSet<>(bookIds).size()) {
            throw new IllegalArgumentException("Duplicate book IDs are not allowed in a single borrow slip");
        }

        // Validate all books exist and are in stock
        List<Book> books = new ArrayList<>();
        for (Long bookId : request.bookIds()) {
            Book book = bookRepository
                    .findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book with id " + bookId + " not found"));
            if (book.getQuantity() <= 0) {
                throw new ConflictException("Book is out of stock: " + bookId);
            }
            books.add(book);
        }

        // Build the slip
        BorrowSlip slip = new BorrowSlip();
        slip.setReader(reader);
        slip.setBorrowDate(LocalDate.now());
        slip.setExpectedReturnDate(LocalDate
                .now()
                .plusDays(7));

        // Build slip books and decrement quantity
        for (Book book : books) {
            BorrowSlipBookId id = new BorrowSlipBookId();
            id.setSlipId(null); // Hibernate will fill this after slip is persisted via @MapsId
            id.setBookId(book.getBookId());

            BorrowSlipBook slipBook = new BorrowSlipBook();
            slipBook.setId(id);
            slipBook.setBorrowSlip(slip);
            slipBook.setBook(book);

            slip
                    .getBorrowSlipBooks()
                    .add(slipBook);
            book.setQuantity(book.getQuantity() - 1);
        }

        // Save and return new slip
        BorrowSlip saved = borrowSlipRepository.save(slip);
        log.info("Borrow slip created: slipId={}, readerId={}, bookIds={}",
                saved.getSlipId(), request.readerId(), request.bookIds());
        return saved;
    }

    @Transactional(readOnly = true)
    @Override
    public BorrowSlipResponse findById(Long slipId) {
        return borrowSlipRepository
                .findByIdWithDetails(slipId)
                .map(BorrowSlipResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow slip with id " + slipId + " not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public List<BorrowSlipResponse> getAllBorrowSlips() {
        return borrowSlipRepository
                .findAllWithDetails()
                .stream()
                .map(BorrowSlipResponse::from)
                .toList();
    }
}
