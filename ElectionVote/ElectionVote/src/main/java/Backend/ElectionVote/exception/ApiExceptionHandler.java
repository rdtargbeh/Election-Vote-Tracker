package Backend.ElectionVote.exception;

import Backend.ElectionVote.security.TenantFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.LinkedHashMap;


import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@org.springframework.web.bind.annotation.RestControllerAdvice
public class ApiExceptionHandler {

    /* ---------- 404 ---------- */
    @org.springframework.web.bind.annotation.ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null, ex, false);
    }

    /* ---------- 400 family ---------- */
    @org.springframework.web.bind.annotation.ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req, null, ex, false);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(TenantFilter.BadTenantSelectionException.class)
    public ResponseEntity<ApiError> handleBadTenant(TenantFilter.BadTenantSelectionException ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req, null, ex, false);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value"),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Validation failed", req, fieldErrors, ex, false);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> Optional.ofNullable(v.getMessage()).orElse("Invalid value"),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Validation failed", req, fieldErrors, ex, false);
    }

    /* ---------- 405 ---------- */
    @org.springframework.web.bind.annotation.ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), req, null, ex, false);
    }

    /* ---------- 409 ---------- */
    @org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        return respond(HttpStatus.CONFLICT, "CONFLICT",
                "Request violates a data constraint", req, null, ex, true);
    }

    /* ---------- 401 / 403 ---------- */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return respond(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                (ex.getMessage() != null ? ex.getMessage() : "Authentication failed"),
                req,
                null,
                ex,
                false
        );
    }

//    @org.springframework.web.bind.annotation.ExceptionHandler(AuthenticationException.class)
//    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
//        return respond(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), req, null, ex, false);
//    }

    @org.springframework.web.bind.annotation.ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), req, null, ex, false);
    }

    /* ---------- 500 fallback ---------- */
    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", req, null, ex, true);
    }



    /* ---------- helper ---------- */
    private ResponseEntity<ApiError> respond(HttpStatus status,
                                             String errorCode,
                                             String message,
                                             HttpServletRequest req,
                                             Map<String, String> fieldErrors,
                                             Exception ex,
                                             boolean logStack) {
        String path = (req != null) ? req.getRequestURI() : null;
        String rid  = MDC.get("rid").toString();

        if (logStack) log.error("{} {} -> {} {}: {}", safe(req.getMethod()), path, status.value(), errorCode, message, ex);
        else          log.warn ("{} {} -> {} {}: {}", safe(req.getMethod()), path, status.value(), errorCode, message);

        ApiError body = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .error(errorCode)
                .message(Optional.ofNullable(message).orElse(status.getReasonPhrase()))
                .path(path)
                .rid(rid)
                .fieldErrors(fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private static String safe(String s) { return s == null ? "" : s; }



}

