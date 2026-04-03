package io.libry.service;

import io.libry.dto.slip.BorrowSlipRequest;
import io.libry.dto.slip.BorrowSlipResponse;
import io.libry.dto.slip.ReturnSlipRequest;
import io.libry.dto.slip.ReturnSlipResponse;
import io.libry.entity.BorrowSlip;

import java.util.List;

public interface BorrowSlipService {
    BorrowSlip createBorrowSlip(BorrowSlipRequest request);

    BorrowSlipResponse findById(Long slipId);

    List<BorrowSlipResponse> getAllBorrowSlips();

    ReturnSlipResponse returnSlip(Long slipId, ReturnSlipRequest request);
}
