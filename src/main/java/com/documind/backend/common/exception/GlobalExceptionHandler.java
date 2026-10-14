package com.documind.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String INVALID = "invalid";

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
		ErrorCode code = ex.getErrorCode();
		if (code.getStatus().is5xxServerError()) {
			log.error("Server-side API error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		} else {
			log.debug("API error {} on {} {}", code, request.getMethod(), request.getRequestURI());
		}
		return build(code, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> details = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(e -> details.putIfAbsent(e.getField(), message(e.getDefaultMessage())));
		ex.getBindingResult().getGlobalErrors()
				.forEach(e -> details.putIfAbsent(e.getObjectName(), message(e.getDefaultMessage())));
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, details);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex,
														   HttpServletRequest request) {
		Map<String, String> details = new LinkedHashMap<>();
		ex.getParameterValidationResults().forEach(result -> {
			if (result instanceof ParameterErrors parameterErrors) {
				parameterErrors.getFieldErrors().forEach(fe ->
						details.putIfAbsent(fe.getField(), message(fe.getDefaultMessage())));
				parameterErrors.getGlobalErrors().forEach(ge ->
						details.putIfAbsent(ge.getObjectName(), message(ge.getDefaultMessage())));
			} else {
				MethodParameter parameter = result.getMethodParameter();
				String name = Objects.requireNonNullElse(
						parameter.getParameterName(), "arg" + parameter.getParameterIndex());
				result.getResolvableErrors().forEach(err ->
						details.putIfAbsent(name, message(err.getDefaultMessage())));
			}
		});
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, details);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
	                                                   HttpServletRequest request) {
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request,
				Map.of(ex.getName(), INVALID));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
	                                                   HttpServletRequest request) {
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request,
				Map.of(ex.getParameterName(), "is required"));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.getDefaultMessage(), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
	                                                         HttpServletRequest request) {
		ErrorCode code = ErrorCode.METHOD_NOT_ALLOWED;
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(code.getStatus());
		Set<HttpMethod> allowed = ex.getSupportedHttpMethods();
		if (allowed != null && !allowed.isEmpty()) {
			builder.allow(allowed.toArray(HttpMethod[]::new));
		}
		return builder.body(ApiError.of(code, code.getDefaultMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
	                                                            HttpServletRequest request) {
		return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE.getDefaultMessage(), request);
	}

	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<ApiError> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
	                                                             HttpServletRequest request) {
		return build(ErrorCode.NOT_ACCEPTABLE, ErrorCode.NOT_ACCEPTABLE.getDefaultMessage(), request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getDefaultMessage(), request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
		return build(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.getDefaultMessage(), request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean anonymous = auth == null || auth instanceof AnonymousAuthenticationToken;
		ErrorCode code = anonymous ? ErrorCode.UNAUTHORIZED : ErrorCode.FORBIDDEN;
		return build(code, code.getDefaultMessage(), request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex,
	                                                     HttpServletRequest request) {
		log.warn("Optimistic lock failure on {} {}", request.getMethod(), request.getRequestURI());
		return build(ErrorCode.CONCURRENT_MODIFICATION, ErrorCode.CONCURRENT_MODIFICATION.getDefaultMessage(), request);
	}

	@ExceptionHandler(PessimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handlePessimisticLock(PessimisticLockingFailureException ex,
	                                                      HttpServletRequest request) {
		log.warn("Could not acquire lock on {} {}", request.getMethod(), request.getRequestURI(), ex);
		ErrorCode code = ErrorCode.TEMPORARILY_UNAVAILABLE;
		return ResponseEntity.status(code.getStatus())
				.header(HttpHeaders.RETRY_AFTER, "1")
				.body(ApiError.of(code, code.getDefaultMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request);
	}

	private static String message(String defaultMessage) {
		return Objects.requireNonNullElse(defaultMessage, INVALID);
	}

	private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request) {
		return ResponseEntity.status(code.getStatus())
				.body(ApiError.of(code, message, request.getRequestURI()));
	}

	private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request,
	                                       Map<String, String> details) {
		return ResponseEntity.status(code.getStatus())
				.body(ApiError.of(code, message, request.getRequestURI(), details));
	}
}