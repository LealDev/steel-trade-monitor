package com.steeltrade.shared.exception;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(ExternalSourceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalSource(ExternalSourceException ex,
                                                                 HttpServletRequest request) {
        log.error("Falha de comunicação com fonte externa", ex);
        return build(HttpStatus.BAD_GATEWAY, "EXTERNAL_SOURCE_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             HttpServletRequest request) {
        String detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detalhes, request);
    }

    /** Constraints em @RequestParam (@Min/@Max via @Validated) são 400, não 500. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        String detalhes = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detalhes, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleParamValidation(HandlerMethodValidationException ex,
                                                                  HttpServletRequest request) {
        String detalhes = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream())
                .map(erro -> String.valueOf(erro.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", detalhes, request);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String message,
                                                   HttpServletRequest request) {
        var body = new ApiErrorResponse(OffsetDateTime.now(clock), status.value(), code, message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
