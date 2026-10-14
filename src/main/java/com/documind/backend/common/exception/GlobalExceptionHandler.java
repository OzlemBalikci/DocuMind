package com.documind.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
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


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
		ErrorCode code = ex.getErrorCode();
		if (code.getStatus().is5xxServerError()) {
			log.error("API error: {}", code, ex);
		} else {
			log.debug("API error: {} - {}", code, ex.getMessage());
		}
		return build(code, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> details = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(e -> details.putIfAbsent(e.getField(), e.getDefaultMessage()));
		ex.getBindingResult().getGlobalErrors()
				.forEach(e -> details.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, details);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
		Map<String, String> details = new LinkedHashMap<>();
		ex.getParameterValidationResults().forEach(r ->
				r.getResolvableErrors().forEach(err ->
						details.putIfAbsent(r.getMethodParameter().getParameterName(), err.getDefaultMessage())));
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), request, details);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.getDefaultMessage(), request);
	}

	@ExceptionHandler({MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class})
	public ResponseEntity<ApiError> handleBadParam(Exception ex, HttpServletRequest request) {
		return build(ErrorCode.VALIDATION_ERROR, ex.getMessage(), request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
		return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), request);
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
		return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.getDefaultMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error", ex);
		return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request);
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
