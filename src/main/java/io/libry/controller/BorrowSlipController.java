package io.libry.controller;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.slip.BorrowSlipRequest;
import io.libry.dto.slip.BorrowSlipResponse;
import io.libry.dto.slip.ReturnSlipRequest;
import io.libry.dto.slip.ReturnSlipResponse;
import io.libry.entity.BorrowSlip;
import io.libry.service.BorrowSlipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/borrow-slips")
@RequiredArgsConstructor
public class BorrowSlipController {
    private final BorrowSlipService borrowSlipService;

    @PostMapping
    public ResponseEntity<Void> createBorrowSlip(@Valid @RequestBody BorrowSlipRequest request) {
        BorrowSlip borrowSlip = borrowSlipService.createBorrowSlip(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(borrowSlip.getSlipId())
                .toUri();

        return ResponseEntity
                .created(location)
                .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BorrowSlipResponse> getBorrowSlipById(@PathVariable("id") Long slipId) {
        return ResponseEntity.ok(borrowSlipService.findById(slipId));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<BorrowSlipResponse>> getAllBorrowSlips(@PageableDefault(sort = "slipId") Pageable pageable) {
        return ResponseEntity.ok(borrowSlipService.getAllBorrowSlips(pageable));
    }

    @PatchMapping("/{id}/return")
    public ResponseEntity<ReturnSlipResponse> returnSlip(@PathVariable("id") Long slipId, @Valid @RequestBody ReturnSlipRequest request) {
        return ResponseEntity.ok(borrowSlipService.returnSlip(slipId, request));
    }
}
