package io.libry.service;

import io.libry.dto.reader.PatchReaderRequest;
import io.libry.dto.reader.ReaderRequest;
import io.libry.dto.reader.ReaderResponse;
import io.libry.entity.Reader;
import io.libry.exception.ResourceNotFoundException;
import io.libry.repository.ReaderRepository;
import io.libry.service.impl.ReaderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReaderServiceImplTest {

    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private ReaderServiceImpl readerService;

    private Reader reader;

    @BeforeEach
    void setUp() {
        reader = new Reader();
        reader.setReaderId(1L);
        reader.setFullName("Thomas Shelby");
        reader.setIdCardNumber("84839281423");
        reader.setDob(LocalDate.of(1990, 1, 1));
        reader.setEmail("thomas@example.com");
        reader.setExpiryDate(LocalDate
                .now()
                .plusYears(2));
    }

    // --- getAllReaders ---

    @Test
    void getAllReaders_returnsAllReaders() {
        when(readerRepository.findAll()).thenReturn(List.of(reader));

        List<ReaderResponse> result = readerService.getAllReaders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Thomas Shelby");
        assertThat(result.get(0).idCardNumber()).isEqualTo("84839281423");
    }

    @Test
    void getAllReaders_returnsEmptyList_whenNoReaders() {
        when(readerRepository.findAll()).thenReturn(List.of());

        List<ReaderResponse> result = readerService.getAllReaders();

        assertThat(result).isEmpty();
    }

    // --- createReader ---

    @Test
    void createReader_savesAndReturnsReader() {
        ReaderRequest request = new ReaderRequest(
                "Thomas Shelby", "84839281423",
                LocalDate.of(1990, 1, 1), null,
                "thomas@example.com", null,
                LocalDate.now().plusYears(2));

        when(readerRepository.save(any(Reader.class))).thenReturn(reader);

        ReaderResponse result = readerService.createReader(request);

        assertThat(result.fullName()).isEqualTo("Thomas Shelby");
        assertThat(result.idCardNumber()).isEqualTo("84839281423");
        verify(readerRepository).save(any(Reader.class));
    }

    // --- findById ---

    @Test
    void findById_returnsReader_whenFound() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));

        ReaderResponse result = readerService.findById(1L);

        assertThat(result.readerId()).isEqualTo(1L);
        assertThat(result.fullName()).isEqualTo("Thomas Shelby");
    }

    @Test
    void findById_throws404_whenNotFound() {
        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- findByIdCardNumber ---

    @Test
    void findByIdCardNumber_returnsReader_whenFound() {
        when(readerRepository.findByIdCardNumber("84839281423")).thenReturn(Optional.of(reader));

        ReaderResponse result = readerService.findByIdCardNumber("84839281423");

        assertThat(result.idCardNumber()).isEqualTo("84839281423");
    }

    @Test
    void findByIdCardNumber_throws404_whenNotFound() {
        when(readerRepository.findByIdCardNumber("000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.findByIdCardNumber("000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("000");
    }

    // --- findByFullName ---

    @Test
    void findByFullName_returnsMatchingReaders() {
        when(readerRepository.findByFullNameContainingIgnoreCase("thomas")).thenReturn(List.of(reader));

        List<ReaderResponse> result = readerService.findByFullName("thomas");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Thomas Shelby");
    }

    @Test
    void findByFullName_returnsEmptyList_whenNoMatch() {
        when(readerRepository.findByFullNameContainingIgnoreCase("xyz")).thenReturn(List.of());

        List<ReaderResponse> result = readerService.findByFullName("xyz");

        assertThat(result).isEmpty();
    }

    // --- putReader ---

    @Test
    void putReader_updatesAllFields() {
        ReaderRequest request = new ReaderRequest(
                "Arthur Shelby", "99999999999",
                LocalDate.of(1985, 5, 10), "Male",
                "arthur@example.com", "Birmingham",
                LocalDate
                        .now()
                        .plusYears(3));

        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));

        readerService.putReader(1L, request);

        assertThat(reader.getFullName()).isEqualTo("Arthur Shelby");
        assertThat(reader.getIdCardNumber()).isEqualTo("99999999999");
        assertThat(reader.getEmail()).isEqualTo("arthur@example.com");
        verify(readerRepository).save(reader);
    }

    @Test
    void putReader_throws404_whenNotFound() {
        ReaderRequest request = new ReaderRequest(
                "Arthur Shelby", "99999999999",
                LocalDate.of(1985, 5, 10), null, null, null,
                LocalDate
                        .now()
                        .plusYears(3));

        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.putReader(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(readerRepository, never()).save(any());
    }

    // --- patchReader ---

    @Test
    void patchReader_updatesOnlyProvidedFields() {
        PatchReaderRequest request = new PatchReaderRequest(
                "Arthur Shelby", null, null, null, null, null, null);

        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));

        readerService.patchReader(1L, request);

        assertThat(reader.getFullName()).isEqualTo("Arthur Shelby");
        assertThat(reader.getIdCardNumber()).isEqualTo("84839281423"); // unchanged
        assertThat(reader.getEmail()).isEqualTo("thomas@example.com"); // unchanged
        verify(readerRepository).save(reader);
    }

    @Test
    void patchReader_throws404_whenNotFound() {
        PatchReaderRequest request = new PatchReaderRequest(
                "Arthur Shelby", null, null, null, null, null, null);

        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readerService.patchReader(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(readerRepository, never()).save(any());
    }

    // --- deleteReader ---

    @Test
    void deleteReader_deletesReader_whenFound() {
        when(readerRepository.existsById(1L)).thenReturn(true);

        readerService.deleteReader(1L);

        verify(readerRepository).deleteById(1L);
    }

    @Test
    void deleteReader_throws404_whenNotFound() {
        when(readerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> readerService.deleteReader(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(readerRepository, never()).deleteById(any());
    }
}
