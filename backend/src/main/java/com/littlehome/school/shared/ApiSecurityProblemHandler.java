package com.littlehome.school.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public ApiSecurityProblemHandler(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        writeProblem(request, response, HttpStatus.UNAUTHORIZED, "Authentication required", "UNAUTHENTICATED");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) throws IOException {
        writeProblem(request, response, HttpStatus.FORBIDDEN, "Access denied", "FORBIDDEN");
    }

    private void writeProblem(HttpServletRequest request, HttpServletResponse response,
                              HttpStatus status, String title, String code) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, title + ".");
        problem.setType(URI.create("https://api.littlehome.example/problems/" + code.toLowerCase()));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        String requestId = request.getHeader("X-Request-Id");
        problem.setProperty("requestId", requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId);
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}