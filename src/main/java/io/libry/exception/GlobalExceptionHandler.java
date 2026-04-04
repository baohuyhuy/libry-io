package io.libry.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final PropertyNamingStrategies.SnakeCaseStrategy SNAKE_CASE =
            new PropertyNamingStrategies.SnakeCaseStrategy();

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex
                .getBindingResult()
                .getAllErrors()
                .forEach((error) -> {
                    String fieldName = SNAKE_CASE.translate(((FieldError) error).getField());
                    String errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                });
        return errors;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, String> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));
            String snakeField = SNAKE_CASE.translate(fieldName);

            if (ife.getTargetType() != null && ife.getTargetType().isAssignableFrom(LocalDate.class)) {
                return Map.of(snakeField, "Invalid date format. Use YYYY-MM-DD");
            }
            return Map.of(snakeField, "Invalid value: " + ife.getValue());
        }

        if (cause instanceof UnrecognizedPropertyException upe) {
            String snakeField = SNAKE_CASE.translate(upe.getPropertyName());
            return Map.of("error", "Unknown field: " + snakeField);
        }

        return Map.of("error", "Malformed JSON request");
    }

    // Handles invalid path variable types, e.g. /readers/abc instead of /readers/1
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Map<String, String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return Map.of("error", "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Expected type: " + expected);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Map<String, String> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fullPath = violation.getPropertyPath().toString();
            // propertyPath is like "save.reader.email" — take only the field name
            String fieldName = fullPath.contains(".") ? fullPath.substring(fullPath.lastIndexOf('.') + 1) : fullPath;
            String snakeField = SNAKE_CASE.translate(fieldName);
            errors.put(snakeField, violation.getMessage());
        });
        return errors;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(BadCredentialsException.class)
    public Map<String, String> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Failed login attempt: bad credentials");
        return Map.of("error", "Invalid username or password");
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public Map<String, String> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(UnprocessableEntityException.class)
    public Map<String, String> handleUnprocessableEntity(UnprocessableEntityException ex) {
        log.warn("Unprocessable entity: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PropertyReferenceException.class)
    public Map<String, String> handleInvalidSortField(PropertyReferenceException ex) {
        String field = SNAKE_CASE.translate(ex.getPropertyName());
        log.warn("Invalid sort field: {}", ex.getPropertyName());
        return Map.of("error", "Invalid sort field: " + field);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public Map<String, String> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Map<String, String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation: {}", message);

        if (message.contains("id_card_number")) {
            return Map.of("id_card_number", "ID card number already exists");
        }
        if (message.contains("email")) {
            return Map.of("email", "Email already exists");
        }
        if (message.contains("isbn")) {
            return Map.of("isbn", "ISBN already exists");
        }

        return Map.of("error", "A unique constraint was violated");
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, String> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return Map.of("error", "An unexpected error occurred");
    }
}
