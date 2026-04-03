package io.libry.service.impl;

import io.libry.dto.reader.PatchReaderRequest;
import io.libry.dto.reader.ReaderRequest;
import io.libry.dto.reader.ReaderResponse;
import io.libry.entity.Reader;
import io.libry.repository.ReaderRepository;
import io.libry.service.ReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {
    private final ReaderRepository readerRepository;

    @Override
    public List<ReaderResponse> getAllReaders() {
        return readerRepository
                .findAll()
                .stream()
                .map(ReaderResponse::from)
                .toList();
    }

    @Transactional
    @Override
    public ReaderResponse createReader(ReaderRequest request) {
        Reader newReader = new Reader();
        populateReaderDetails(request, newReader);

        return ReaderResponse.from(readerRepository.save(newReader));
    }

    @Transactional
    @Override
    public void putReader(Long readerId, ReaderRequest request) {
        Reader existingReader = readerRepository
                .findById(readerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        populateReaderDetails(request, existingReader);

        readerRepository.save(existingReader);
    }

    @Transactional
    @Override
    public void patchReader(Long readerId, PatchReaderRequest request) {
        Reader existingReader = readerRepository
                .findById(readerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (request.fullName() != null) {
            existingReader.setFullName(request.fullName());
        }
        if (request.idCardNumber() != null) {
            existingReader.setIdCardNumber(request.idCardNumber());
        }
        if (request.dob() != null) {
            existingReader.setDob(request.dob());
        }
        if (request.gender() != null) {
            existingReader.setGender(request.gender());
        }
        if (request.email() != null) {
            existingReader.setEmail(request.email());
        }
        if (request.address() != null) {
            existingReader.setAddress(request.address());
        }
        if (request.expiryDate() != null) {
            existingReader.setExpiryDate(request.expiryDate());
        }

        readerRepository.save(existingReader);
    }

    @Transactional
    @Override
    public void deleteReader(Long readerId) {
        if (!readerRepository.existsById(readerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        readerRepository.deleteById(readerId);
    }

    @Override
    public ReaderResponse findById(Long readerId) {
        return readerRepository
                .findById(readerId)
                .map(ReaderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public ReaderResponse findByIdCardNumber(String idCardNumber) {
        return readerRepository
                .findByIdCardNumber(idCardNumber)
                .map(ReaderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Override
    public List<ReaderResponse> findByFullName(String fullName) {
        return readerRepository
                .findByFullNameContainingIgnoreCase(fullName)
                .stream()
                .map(ReaderResponse::from)
                .toList();
    }

    static private void populateReaderDetails(ReaderRequest request,
                                              Reader newReader) {
        newReader.setFullName(request.fullName());
        newReader.setIdCardNumber(request.idCardNumber());
        newReader.setDob(request.dob());
        newReader.setGender(request.gender());
        newReader.setEmail(request.email());
        newReader.setAddress(request.address());
        newReader.setExpiryDate(request.expiryDate());
    }
}
