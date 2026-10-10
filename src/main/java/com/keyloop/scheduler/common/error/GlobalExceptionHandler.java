package com.keyloop.scheduler.common.error;

import com.keyloop.scheduler.common.error.ApiException.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex) {
        return respond(ex.code(), ex.getMessage(), ex.errors());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(ErrorCode.INTERNAL_ERROR, null, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.add(new FieldError(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errors.add(new FieldError(e.getObjectName(), e.getDefaultMessage())));
        return respond(ErrorCode.VALIDATION_FAILED, null, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(e -> {
                String field = e instanceof org.springframework.validation.FieldError fe ? fe.getField() : name;
                errors.add(new FieldError(field, e.getDefaultMessage()));
            });
        });
        return respond(ErrorCode.VALIDATION_FAILED, null, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return respond(ErrorCode.VALIDATION_FAILED, "The request body is missing or is not valid JSON.",
                List.of(new FieldError("body", "must be a valid JSON object")));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return respond(ErrorCode.VALIDATION_FAILED, null,
                List.of(new FieldError(field, "has an invalid format")));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        return respond(ErrorCode.VALIDATION_FAILED, null,
                List.of(new FieldError(ex.getParameterName(), "is required")));
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex instanceof MissingRequestHeaderException h
                && IDEMPOTENCY_KEY_HEADER.equalsIgnoreCase(h.getHeaderName())) {
            return respond(ErrorCode.IDEMPOTENCY_KEY_MISSING, null, List.of());
        }
        String field = ex instanceof MissingRequestHeaderException h ? h.getHeaderName() : "request";
        return respond(ErrorCode.VALIDATION_FAILED, null, List.of(new FieldError(field, "is invalid or missing")));
    }

    /** Other framework exceptions (404, 405, 415 ...) keep Spring's status; only the requestId is added. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail pd) {
            pd.setProperty(REQUEST_ID_KEY, MDC.get(REQUEST_ID_KEY));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private ResponseEntity<Object> respond(ErrorCode code, @Nullable String detail, List<FieldError> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(code.status(),
                detail != null ? detail : code.defaultDetail());
        pd.setProperty("code", code.name());
        pd.setProperty(REQUEST_ID_KEY, MDC.get(REQUEST_ID_KEY));
        if (!errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }
        return ResponseEntity.status(code.status()).body(pd);
    }
}
