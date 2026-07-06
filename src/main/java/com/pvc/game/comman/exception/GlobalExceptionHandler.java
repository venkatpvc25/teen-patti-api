package com.pvc.game.comman.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import com.pvc.game.comman.response.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {

                log.warn("Business exception: {}", ex.getMessage(), ex);

                return ResponseEntity
                                .badRequest()
                                .body(new ApiError(
                                                false,
                                                ex.getErrorCode().name(),
                                                ex.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {

                String errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                log.warn("Validation exception: {}", errors, ex);

                return ResponseEntity
                                .badRequest()
                                .body(new ApiError(
                                                false,
                                                ErrorCode.VALIDATION_ERROR.name(),
                                                errors));
        }

        @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
        public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex) {

                log.warn("Bad request: {}", ex.getMessage(), ex);

                return ResponseEntity
                                .badRequest()
                                .body(new ApiError(
                                                false,
                                                ErrorCode.VALIDATION_ERROR.name(),
                                                ex.getMessage()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleGeneral(Exception ex) {

                log.error("Unhandled exception", ex);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(new ApiError(
                                                false,
                                                ErrorCode.INTERNAL_ERROR.name(),
                                                "Something went wrong"));
        }
}
