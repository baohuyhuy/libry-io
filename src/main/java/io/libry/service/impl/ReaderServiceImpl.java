package io.libry.service.impl;

import io.libry.dto.PatchReaderRequest;
import io.libry.dto.PutReaderRequest;
import io.libry.entity.Reader;
import io.libry.repository.ReaderRepository;
import io.libry.service.ReaderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReaderServiceImpl implements ReaderService {
    private final ReaderRepository readerRepository;

    public ReaderServiceImpl(ReaderRepository readerRepository) {
        this.readerRepository = readerRepository;
    }

    @Override
    public List<Reader> getAllReaders() {
        return readerRepository.findAll();
    }

    @Override
    public Reader createReader(Reader reader) {
        return readerRepository.save(reader);
    }

    @Override
    public void putReader(Long readerId, PutReaderRequest request) {
        Reader existingReader = readerRepository
                .findById(readerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        existingReader.setFullName(request.fullName());
        existingReader.setIdCardNumber(request.idCardNumber());
        existingReader.setDob(request.dob());
        existingReader.setGender(request.gender());
        existingReader.setEmail(request.email());
        existingReader.setAddress(request.address());
        existingReader.setExpiryDate(request.expiryDate());

        readerRepository.save(existingReader);
    }

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
}
