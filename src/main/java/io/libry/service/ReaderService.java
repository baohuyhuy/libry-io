package io.libry.service;

import io.libry.dto.PatchReaderRequest;
import io.libry.dto.PutReaderRequest;
import io.libry.entity.Reader;

import java.util.List;

public interface ReaderService {
    List<Reader> getAllReaders();

    Reader createReader(Reader reader);

    void putReader(Long readerId, PutReaderRequest request);

    void patchReader(Long readerId, PatchReaderRequest request);

    void deleteReader(Long readerId);

    Reader findById(Long readerId);

    Reader findByIdCardNumber(String idCardNumber);

    List<Reader> findByFullName(String fullName);
}
