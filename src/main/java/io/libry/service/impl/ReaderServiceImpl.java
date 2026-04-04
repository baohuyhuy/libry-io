package io.libry.service.impl;

import io.libry.dto.PaginatedResponse;
import io.libry.dto.reader.PatchReaderRequest;
import io.libry.dto.reader.ReaderRequest;
import io.libry.dto.reader.ReaderResponse;
import io.libry.entity.Reader;
import io.libry.exception.ResourceNotFoundException;
import io.libry.repository.ReaderRepository;
import io.libry.service.ReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {
    private final ReaderRepository readerRepository;

    @Override
    public PaginatedResponse<ReaderResponse> getAllReaders(Pageable pageable) {
        return PaginatedResponse.from(
                readerRepository.findAll(pageable).map(ReaderResponse::from)
        );
    }

    @Transactional
    @Override
    public ReaderResponse createReader(ReaderRequest request) {
        log.info("Creating reader: email={}", request.email());
        Reader newReader = new Reader();
        populateReaderDetails(request, newReader);

        ReaderResponse response = ReaderResponse.from(readerRepository.save(newReader));
        log.info("Reader created: readerId={}, email={}", response.readerId(), request.email());
        return response;
    }

    @Transactional
    @Override
    public void putReader(Long readerId, ReaderRequest request) {
        log.info("Replacing reader: readerId={}", readerId);
        Reader existingReader = readerRepository
                .findById(readerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reader with id " + readerId + " not found"));

        populateReaderDetails(request, existingReader);

        readerRepository.save(existingReader);
        log.info("Reader replaced: readerId={}", readerId);
    }

    @Transactional
    @Override
    public void patchReader(Long readerId, PatchReaderRequest request) {
        log.info("Patching reader: readerId={}", readerId);
        Reader existingReader = readerRepository
                .findById(readerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reader with id " + readerId + " not found"));

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
        log.info("Reader patched: readerId={}", readerId);
    }

    @Transactional
    @Override
    public void deleteReader(Long readerId) {
        log.info("Deleting reader: readerId={}", readerId);
        if (!readerRepository.existsById(readerId)) {
            throw new ResourceNotFoundException("Reader with id " + readerId + " not found");
        }
        readerRepository.deleteById(readerId);
        log.info("Reader deleted: readerId={}", readerId);
    }

    @Override
    public ReaderResponse findById(Long readerId) {
        return readerRepository
                .findById(readerId)
                .map(ReaderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Reader with id " + readerId + " not found"));
    }

    @Override
    public ReaderResponse findByIdCardNumber(String idCardNumber) {
        return readerRepository
                .findByIdCardNumber(idCardNumber)
                .map(ReaderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Reader with id card number " + idCardNumber + " not found"));
    }

    @Override
    public PaginatedResponse<ReaderResponse> findByFullName(String fullName, Pageable pageable) {
        return PaginatedResponse.from(
                readerRepository.findByFullNameContainingIgnoreCase(fullName, pageable).map(ReaderResponse::from)
        );
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
