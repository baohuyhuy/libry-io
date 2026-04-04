package io.libry.service;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.reader.PatchReaderRequest;
import io.libry.dto.reader.ReaderRequest;
import io.libry.dto.reader.ReaderResponse;
import org.springframework.data.domain.Pageable;

public interface ReaderService {
    PaginatedResponse<ReaderResponse> getAllReaders(Pageable pageable);

    ReaderResponse createReader(ReaderRequest reader);

    void putReader(Long readerId, ReaderRequest request);

    void patchReader(Long readerId, PatchReaderRequest request);

    void deleteReader(Long readerId);

    ReaderResponse findById(Long readerId);

    ReaderResponse findByIdCardNumber(String idCardNumber);

    PaginatedResponse<ReaderResponse> findByFullName(String fullName, Pageable pageable);
}
