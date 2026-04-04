package io.libry.service.impl;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.slip.BorrowSlipRequest;
import io.libry.dto.slip.BorrowSlipResponse;
import io.libry.dto.slip.ReturnSlipRequest;
import io.libry.dto.slip.ReturnSlipResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public PaginatedResponse<BorrowSlipResponse> getAllBorrowSlips(Pageable pageable) {
        var idPage = borrowSlipRepository.findPageOfIds(pageable);
        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return PaginatedResponse.from(idPage.map(id -> (BorrowSlipResponse) null));
        }
        Map<Long, BorrowSlipResponse> byId = borrowSlipRepository.findAllWithDetailsByIds(ids)
                .stream()
                .collect(Collectors.toMap(BorrowSlip::getSlipId, BorrowSlipResponse::from));
        List<BorrowSlipResponse> responses = ids.stream()
                .map(byId::get)
                .collect(Collectors.toList());
        return PaginatedResponse.from(new PageImpl<>(responses, pageable, idPage.getTotalElements()));
    }

    @Transactional
    @Override
    public ReturnSlipResponse returnSlip(Long slipId, ReturnSlipRequest request) {
        log.info("Returning borrow slip: slipId={}, lostBookIds={}", slipId, request.lostBookIds());

        // Validate book IDs are unique within the request
        List<Long> lostBookIds = request.lostBookIds();
        if (lostBookIds.size() != new HashSet<>(lostBookIds).size()) {
            throw new IllegalArgumentException("Duplicate book IDs in lostBookIds");
        }

        // Load the slip by ID
        BorrowSlip slip = borrowSlipRepository
                .findByIdWithDetails(slipId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrow slip with id " + slipId + " not found"));

        // Validate the slip hasn't already been returned
        if (slip.getActualReturnDate() != null) {
            throw new UnprocessableEntityException("Borrow slip has already been returned");
        }

        // Validate all lostBookIds belong to this slip
        for (Long lostBookId : lostBookIds) {
            boolean found = slip
                    .getBorrowSlipBooks()
                    .stream()
                    .anyMatch(sb -> sb
                            .getBook()
                            .getBookId()
                            .equals(lostBookId));
            if (!found) {
                throw new IllegalArgumentException("Lost book ID " + lostBookId + " does not belong to this borrow slip");
            }
        }

        // Update the actualReturnDate
        slip.setActualReturnDate(LocalDate.now());

        // Update lost status and Increment quantity for returned books
        List<BorrowSlipBook> lostBooks = new ArrayList<>();
        Set<Long> lostBookIdSet = new HashSet<>(lostBookIds);
        for (BorrowSlipBook slipBook : slip.getBorrowSlipBooks()) {
            if (lostBookIdSet.contains(slipBook
                    .getBook()
                    .getBookId())) {
                slipBook.setLost(true);
                lostBooks.add(slipBook);
            } else {
                slipBook
                        .getBook()
                        .setQuantity(slipBook
                                .getBook()
                                .getQuantity() + 1);
            }
        }

        // Calculate fines
        long overdueDays = ChronoUnit.DAYS.between(slip.getExpectedReturnDate(), slip.getActualReturnDate());
        overdueDays = Math.max(overdueDays, 0); // negative means returned early

        BigDecimal overdueFine =
                BigDecimal
                        .valueOf(overdueDays)
                        .multiply(BigDecimal.valueOf(5_000)); // 5.000vnd per day


        List<ReturnSlipResponse.LostBookFine> lostBookFines = lostBooks
                .stream()
                .map(sb -> {
                    Book book = sb.getBook();
                    return new ReturnSlipResponse.LostBookFine(
                            book.getBookId(),
                            book.getTitle(),
                            book
                                    .getPrice()
                                    .multiply(BigDecimal.valueOf(2)) // 200%
                    );
                })
                .toList();

        BigDecimal lostFineTotal = lostBookFines
                .stream()
                .map(ReturnSlipResponse.LostBookFine::fine)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFine = overdueFine.add(lostFineTotal);

        log.info("Borrow slip returned: slipId={}, overdueDays={}, overdueFine={}, lostBookFines={}, totalFine={}",
                slipId, overdueDays, overdueFine, lostBookFines, totalFine);

        return new ReturnSlipResponse(
                slip.getSlipId(),
                slip.getBorrowDate(),
                slip.getExpectedReturnDate(),
                slip.getActualReturnDate(),
                overdueDays,
                overdueFine,
                lostBookFines,
                totalFine
        );
    }
}
