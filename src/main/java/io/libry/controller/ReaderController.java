package io.libry.controller;

import io.libry.dto.PatchReaderRequest;
import io.libry.dto.PutReaderRequest;
import io.libry.entity.Reader;
import io.libry.service.ReaderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/readers")
public class ReaderController {
    private final ReaderService readerService;

    public ReaderController(ReaderService readerService) {
        this.readerService = readerService;
    }

    @GetMapping("/")
    public List<Reader> getAllReaders() {
        return readerService.getAllReaders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reader> findById(@PathVariable("id") Long readerId) {
        return ResponseEntity.ok(readerService.findById(readerId));
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(value = "id_card_number", required = false) String idCardNumber,
                                    @RequestParam(value = "full_name", required = false) String fullName) {
        if (idCardNumber != null && fullName != null) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Only one search parameter is allowed at a time"));
        }
        if (idCardNumber != null) {
            return ResponseEntity.ok(readerService.findByIdCardNumber(idCardNumber));
        }
        if (fullName != null) {
            return ResponseEntity.ok(readerService.findByFullName(fullName));
        }
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", "Provide at least one search parameter: id_card_number or full_name"));
    }
    
    @PostMapping("/")
    public ResponseEntity<Void> createReader(@Valid @RequestBody Reader newReader) {
        Reader reader = readerService.createReader(newReader);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{resourceId}")
                .buildAndExpand(reader.getReaderId())
                .toUri();

        return ResponseEntity
                .created(location)
                .build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> putReader(@PathVariable("id") Long readerId,
                                          @Valid @RequestBody PutReaderRequest request) {
        readerService.putReader(readerId, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patchRead(@PathVariable("id") Long readerId,
                                          @Valid @RequestBody PatchReaderRequest request) {
        readerService.patchReader(readerId, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReader(@PathVariable("id") Long readerId) {
        readerService.deleteReader(readerId);
        return ResponseEntity
                .noContent()
                .build();
    }
}
