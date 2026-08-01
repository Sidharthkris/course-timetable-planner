package com.portfolio.timetable.exception;

import com.portfolio.timetable.dto.ErrorDtos.ApiError;
import com.portfolio.timetable.dto.ErrorDtos.ScheduleConflictError;
import com.portfolio.timetable.dto.ScheduleEntryDtos;
import com.portfolio.timetable.service.ScheduleEntryService;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Translates every exception the service layer can throw into the
 * HTTP status code a REST client actually expects: 404 for missing
 * resources, 409 for scheduling conflicts, 400 for validation and
 * bad-argument errors, 403 when an instructor attempts a
 * coordinator-only action. Scoped to {@code controller} (the REST
 * package) via {@code basePackages} so it never intercepts exceptions
 * thrown from the Thymeleaf {@code web} controllers, which need HTML
 * error handling instead of JSON.
 */
@RestControllerAdvice(basePackages = "com.portfolio.timetable.controller")
public class GlobalExceptionHandler {

    private final ScheduleEntryService scheduleEntryService;

    public GlobalExceptionHandler(ScheduleEntryService scheduleEntryService) {
        this.scheduleEntryService = scheduleEntryService;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public ResponseEntity<ScheduleConflictError> handleScheduleConflict(ScheduleConflictException ex) {
        List<ScheduleEntryDtos.Response> conflicts = ex.getConflictingEntries().stream()
                .map(scheduleEntryService::toResponse)
                .collect(Collectors.toList());
        ScheduleConflictError body = new ScheduleConflictError(
                Instant.now(), 409, "Conflict", ex.getMessage(), conflicts);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "Forbidden", "Only a coordinator can perform this action."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Validation Failed", message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Validation Failed", ex.getMessage()));
    }
}
