package io.libry.service;

import io.libry.dto.reader.PatchReaderRequest;
import io.libry.dto.reader.ReaderRequest;
import io.libry.dto.reader.ReaderResponse;

import java.util.List;

public interface ReaderService {
    List<ReaderResponse> getAllReaders();

    ReaderResponse createReader(ReaderRequest reader);

    void putReader(Long readerId, ReaderRequest request);

    void patchReader(Long readerId, PatchReaderRequest request);

    void deleteReader(Long readerId);

    ReaderResponse findById(Long readerId);

    ReaderResponse findByIdCardNumber(String idCardNumber);

    List<ReaderResponse> findByFullName(String fullName);
}
