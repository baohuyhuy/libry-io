package io.libry.service;

import io.libry.dto.borrow.slip.BorrowSlipRequest;
import io.libry.dto.borrow.slip.BorrowSlipResponse;
import io.libry.entity.BorrowSlip;

import java.util.List;

public interface BorrowSlipService {
    BorrowSlip createBorrowSlip(BorrowSlipRequest request);

    BorrowSlipResponse findById(Long slipId);

    List<BorrowSlipResponse> getAllBorrowSlips();
}
