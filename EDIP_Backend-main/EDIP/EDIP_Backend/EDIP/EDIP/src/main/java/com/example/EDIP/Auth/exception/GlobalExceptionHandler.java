package com.example.EDIP.Auth.exception;

import com.example.EDIP.document.exception.DocumentException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.warn("RuntimeException: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "An error occurred");
        response.put("status", 400);
        response.put("error", "Bad Request");

        return ResponseEntity.badRequest().body(response);
    }
    @ExceptionHandler(DocumentException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentException(DocumentException ex) {
        log.warn("DocumentException: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("status", ex.getStatus().value());
        response.put("error", ex.getStatus().getReasonPhrase());

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Validation failed: " + ex.getBindingResult()
                .getFieldError()
                .getDefaultMessage());
        response.put("status", 400);
        response.put("error", "Bad Request");

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("ConstraintViolationException: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Validation failed: " + ex.getMessage());
        response.put("status", 400);
        response.put("error", "Bad Request");

        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("ResponseStatusException: {}", ex.getReason());

        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getReason());
        response.put("status", ex.getStatusCode().value());
        HttpStatus httpStatus = HttpStatus.resolve(ex.getStatusCode().value());
        String reasonPhrase = (httpStatus != null)
                ? httpStatus.getReasonPhrase()
                : "Unknown Error";

        response.put("error", reasonPhrase);

        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled Exception: ", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "An unexpected error occurred");
        response.put("status", 500);
        response.put("error", "Internal Server Error");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMissingBody(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Request body is missing or malformed");
        response.put("status", 400);
        response.put("error", "Bad Request");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(
            AsyncRequestNotUsableException ex
    ) {

        log.warn("Client disconnected before response was sent");
    }
}
