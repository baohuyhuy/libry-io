package io.libry.service;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.slip.BorrowSlipRequest;
import io.libry.dto.slip.BorrowSlipResponse;
import io.libry.dto.slip.ReturnSlipRequest;
import io.libry.dto.slip.ReturnSlipResponse;
import io.libry.entity.BorrowSlip;
import org.springframework.data.domain.Pageable;

public interface BorrowSlipService {
    BorrowSlip createBorrowSlip(BorrowSlipRequest request);

    BorrowSlipResponse findById(Long slipId);

    PaginatedResponse<BorrowSlipResponse> getAllBorrowSlips(Pageable pageable);

    ReturnSlipResponse returnSlip(Long slipId, ReturnSlipRequest request);
}
