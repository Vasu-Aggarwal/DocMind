package com.docmind.backend.exception;

import com.docmind.backend.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        logger.warn("Resource not found : {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.builder().success(false).message(ex.getMessage()).data(null).timestamp(LocalDateTime.now()).build(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<?> handleProcessingError(DocumentProcessingException ex) {
        logger.warn("Document process error : {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.builder().success(false).message(ex.getMessage()).data(null).timestamp(LocalDateTime.now()).build(), HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSize(MaxUploadSizeExceededException ex) {
        logger.warn("File size limit exceeded : {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.builder().success(false).message("File size exceeds the allowed limit (25MB)").data(null).timestamp(LocalDateTime.now()).build(), HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        for(FieldError error: ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return new ResponseEntity<>(ApiResponse.<Map<String, String>>builder().success(false).message("Validation Failed").data(errors).timestamp(LocalDateTime.now()).build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArguments(IllegalArgumentException ex) {
        logger.warn("Illegal Arguments : {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.<Map<String, String>>builder().success(false).message(ex.getMessage()).data(null).timestamp(LocalDateTime.now()).build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
        logger.warn("Unexpected error occurred : {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.<Map<String, String>>builder().success(false).message("An unexpected error occurred: "+ex.getMessage()).data(null).timestamp(LocalDateTime.now()).build(), HttpStatus.BAD_REQUEST);
    }

}
