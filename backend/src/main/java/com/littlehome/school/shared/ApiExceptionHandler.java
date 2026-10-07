package com.littlehome.school.shared;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        problem.setType(URI.create("https://api.littlehome.example/problems/validation"));
        problem.setTitle("Request validation failed");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("requestId", requestId(request));
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage()))
                .toList();
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleCredentials(HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication failed.");
        problem.setType(URI.create("https://api.littlehome.example/problems/unauthorized"));
        problem.setTitle("Authentication failed");
        problem.setProperty("code", "UNAUTHENTICATED");
        problem.setProperty("requestId", requestId(request));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You do not have access to this resource.");
        problem.setType(URI.create("https://api.littlehome.example/problems/forbidden"));
        problem.setTitle("Access denied");
        problem.setProperty("code", "FORBIDDEN");
        problem.setProperty("requestId", requestId(request));
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConflict(HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The requested change conflicts with existing school data.");
        problem.setType(URI.create("https://api.littlehome.example/problems/conflict"));
        problem.setTitle("Resource conflict");
        problem.setProperty("code", "RESOURCE_CONFLICT");
        problem.setProperty("requestId", requestId(request));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        logger.error("Unhandled API error for request {}", request.getRequestURI(), exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed.");
        problem.setType(URI.create("https://api.littlehome.example/problems/internal-error"));
        problem.setTitle("Internal server error");
        problem.setProperty("code", "INTERNAL_ERROR");
        problem.setProperty("requestId", requestId(request));
        return problem;
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    private record FieldError(String field, String message) { }
}