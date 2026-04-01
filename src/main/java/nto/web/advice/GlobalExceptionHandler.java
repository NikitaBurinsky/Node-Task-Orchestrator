package nto.web.advice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import nto.core.utils.exceptions.BadRequestException;
import nto.core.utils.exceptions.DuplicateUsernameException;
import nto.core.utils.exceptions.InvalidRefreshTokenException;
import nto.core.utils.exceptions.ResourceConflictException;
import nto.core.utils.exceptions.ServerBusyException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
        MethodArgumentNotValidException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
            extractFieldErrors(ex.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed",
            extractFieldErrors(ex.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefreshToken(
        InvalidRefreshTokenException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
        AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler({
        BadRequestException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        MissingRequestCookieException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, resolveBadRequestMessage(ex), null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
        ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
    }

    @ExceptionHandler({DuplicateUsernameException.class, ResourceConflictException.class,
        ServerBusyException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler({EntityNotFoundException.class, NoHandlerFoundException.class,
        NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception ex) {
        return buildResponse(HttpStatus.NOT_FOUND, resolveNotFoundMessage(ex), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error,
                                                              Object details) {
        return new ResponseEntity<>(ApiErrorResponseFactory.buildBody(status, error, details),
            status);
    }

    private Map<String, String> extractFieldErrors(Iterable<FieldError> fieldErrors) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : fieldErrors) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return errors;
    }

    private String resolveBadRequestMessage(Exception ex) {
        if (ex instanceof HttpMessageNotReadableException) {
            return "Malformed JSON request";
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatchException) {
            String parameterName = mismatchException.getName();
            Class<?> requiredType = mismatchException.getRequiredType();
            String typeName = requiredType == null ? "required type" : requiredType.getSimpleName();
            return "Invalid value for parameter '" + parameterName + "'. Expected " + typeName;
        }
        if (ex instanceof MissingServletRequestParameterException parameterException) {
            return "Missing request parameter: " + parameterException.getParameterName();
        }
        if (ex instanceof MissingRequestCookieException cookieException) {
            return "Missing request cookie: " + cookieException.getCookieName();
        }
        return ex.getMessage();
    }

    private String resolveNotFoundMessage(Exception ex) {
        if (ex instanceof NoHandlerFoundException noHandlerFoundException) {
            return "No handler found for " + noHandlerFoundException.getHttpMethod() + " "
                + noHandlerFoundException.getRequestURL();
        }
        if (ex instanceof NoResourceFoundException noResourceFoundException) {
            return "Resource not found: " + noResourceFoundException.getResourcePath();
        }
        return ex.getMessage();
    }
}
