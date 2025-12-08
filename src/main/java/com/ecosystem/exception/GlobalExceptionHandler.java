package com.ecosystem.exception;

import com.ecosystem.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String[]> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            
            // 如果字段已存在错误，追加到数组
            if (errors.containsKey(fieldName)) {
                String[] existingErrors = errors.get(fieldName);
                String[] newErrors = new String[existingErrors.length + 1];
                System.arraycopy(existingErrors, 0, newErrors, 0, existingErrors.length);
                newErrors[existingErrors.length] = errorMessage;
                errors.put(fieldName, newErrors);
            } else {
                errors.put(fieldName, new String[]{errorMessage});
            }
        });

        ErrorResponse errorResponse = new ErrorResponse("Validation failed", errors, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "Invalid email or password",
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        Map<String, String[]> errors = new HashMap<>();
        errors.put("email", new String[]{"Email already exists"});
        ErrorResponse errorResponse = new ErrorResponse(
                "Registration failed",
                errors,
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFoundException(ProductNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                null,
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        
        // 根据不同的错误消息返回不同的状态码
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (message != null && (message.contains("Invalid") || message.contains("Unauthorized"))) {
            status = HttpStatus.UNAUTHORIZED;
        }
        
        ErrorResponse errorResponse = new ErrorResponse(message, null, null);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse(
                "An error occurred",
                null,
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

