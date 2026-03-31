package io.libry.service.impl;

import io.libry.dto.reader.PatchReaderRequest;
import io.libry.dto.reader.ReaderRequest;
import io.libry.dto.reader.ReaderResponse;
import io.libry.entity.Reader;
import io.libry.repository.ReaderRepository;
import io.libry.service.ReaderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReaderServiceImpl implements ReaderService {
    private final ReaderRepository readerRepository;

    public ReaderServiceImpl(ReaderRepository readerRepository) {
        this.readerRepository = readerRepository;
    }

    @Override
    public List<ReaderResponse> getAllReaders() {
        return readerRepository.findAll().stream()
                .map(ReaderResponse::from)
                .toList();
    }

    @Override
    public ReaderResponse createReader(ReaderRequest reader) {
        Reader newReader = new Reader();
        populateReaderDetails(newReader, reader.fullName(), reader.idCardNumber(), reader.dob(), reader.gender(), reader.email(), reader.address(), reader.expiryDate());

        return ReaderResponse.from(readerRepository.save(newReader));
    }

    @Override
    public void putReader(Long readerId, ReaderRequest request) {
        Reader existingReader = readerRepository
                .findById(readerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        populateReaderDetails(existingReader, request.fullName(),
                request.idCardNumber(), request.dob(), request.gender(),
                request.email(), request.address(), request.expiryDate());

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
        return readerRepository.findByFullNameContainingIgnoreCase(fullName).stream()
                .map(ReaderResponse::from)
                .toList();
    }

    static private void populateReaderDetails(Reader newReader, String fullName,
                                              String idCardNumber,
                                              LocalDate dob, String gender,
                                              String email, String address,
                                              LocalDate expiryDate) {
        newReader.setFullName(fullName);
        newReader.setIdCardNumber(idCardNumber);
        newReader.setDob(dob);
        newReader.setGender(gender);
        newReader.setEmail(email);
        newReader.setAddress(address);
        newReader.setExpiryDate(expiryDate);
    }
}
