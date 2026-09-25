package com.poly.exceptions;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        return buildError(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), request);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(body);
    }

    /** Thrown when a JPA entity doesn't exist. */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Thrown when the caller lacks permission. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    /** Thrown for any domain / business-rule violation. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /** @Valid / @Validated on @RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ValidationErrorResponse body = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            request.getRequestURI(),
            LocalDateTime.now(),
            fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    /** @Validated on path/query params (constraint violations) */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                cv -> {
                    String path = cv.getPropertyPath().toString();
                    int dot = path.lastIndexOf('.');
                    return dot >= 0 ? path.substring(dot + 1) : path;
                },
                ConstraintViolation::getMessage,
                (a, b) -> a   // keep first message if two constraints fire on same field
            ));

        ValidationErrorResponse body = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Constraint violation",
            request.getRequestURI(),
            LocalDateTime.now(),
            fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    /** Missing required @RequestParam */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Path variable or param with wrong type (e.g. "abc" for a Long) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = "Parameter '" + ex.getName() + "' must be of type " + expected;
        return buildError(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<ErrorResponse> handleTaskRejected(
            TaskRejectedException ex, HttpServletRequest request) {
        return buildError(
            HttpStatus.SERVICE_UNAVAILABLE,
            "The AI service is busy. Please try again shortly.",
            request
        );
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleAsyncTimeout(
            AsyncRequestTimeoutException ex, HttpServletRequest request) {
        return buildError(
            HttpStatus.GATEWAY_TIMEOUT,
            "The AI service took too long to respond.",
            request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}",
            request.getMethod(), request.getRequestURI(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp
    ) {}

    public record ValidationErrorResponse(
        int status,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
    ) {}
}
