package com.documind.backend.common.exception;

import com.documind.backend.common.validation.ValidationMessages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.web.ErrorResponseException;
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
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String RETRY_AFTER_SECONDS = "1";

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest request) {
		ErrorCode code = ex.getErrorCode();
		if (code.getStatus().is5xxServerError()) {
			log.error("Server-side API error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		} else {
			log.debug("API error {} on {} {}", code, request.getMethod(), request.getRequestURI());
		}
		String message = Objects.requireNonNullElse(ex.getMessage(), code.getDefaultMessage());
		ResponseEntity.BodyBuilder response = ResponseEntity.status(code.getStatus());
		if (ex instanceof TooManyRequestsException limited) {
			long seconds = Math.max(1, limited.getRetryAfter().toSeconds());
			response.header(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
		}
		return response.body(ApiError.of(code, message, request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		return validationResponse(ValidationErrors.fromBindingResult(ex.getBindingResult()));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ValidationErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
		List<ValidationErrorResponse.FieldViolation> errors = new ArrayList<>();
		ex.getParameterValidationResults().forEach(result -> {
			if (result instanceof ParameterErrors parameterErrors) {
				parameterErrors.getFieldErrors().forEach(fe ->
						errors.add(new ValidationErrorResponse.FieldViolation(
								fe.getField(), ValidationErrors.message(fe.getDefaultMessage()))));
				parameterErrors.getGlobalErrors().forEach(ge ->
						errors.add(new ValidationErrorResponse.FieldViolation(
								ge.getObjectName(), ValidationErrors.message(ge.getDefaultMessage()))));
			} else {
				MethodParameter parameter = result.getMethodParameter();
				String name = Objects.requireNonNullElse(
						parameter.getParameterName(), "arg" + parameter.getParameterIndex());
				result.getResolvableErrors().forEach(err ->
						errors.add(new ValidationErrorResponse.FieldViolation(
								name, ValidationErrors.message(err.getDefaultMessage()))));
			}
		});
		return validationResponse(errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
		List<ValidationErrorResponse.FieldViolation> errors = new ArrayList<>();
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			errors.add(new ValidationErrorResponse.FieldViolation(
					constraintField(violation), violation.getMessage()));
		}
		return validationResponse(errors);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ValidationErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return validationResponse(List.of(
				new ValidationErrorResponse.FieldViolation(ex.getName(), ValidationMessages.INVALID_VALUE)));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ValidationErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
		return validationResponse(List.of(
				new ValidationErrorResponse.FieldViolation(ex.getParameterName(), ValidationMessages.REQUIRED)));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ValidationErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
		return validationResponse(List.of(
				new ValidationErrorResponse.FieldViolation(ex.getRequestPartName(), ValidationMessages.REQUIRED)));
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ApiError> handleMultipart(MultipartException ex, HttpServletRequest request) {
		log.debug("Multipart error on {} {}", request.getMethod(), request.getRequestURI());
		return build(ErrorCode.MALFORMED_REQUEST, request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Object> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		UnrecognizedPropertyException unrecognized = JacksonUnreadableCause.unrecognizedProperty(ex);
		if (unrecognized != null) {
			Object body = ValidationErrorResponse.of(
					ValidationMessages.VALIDATION_FAILED,
					List.of(new ValidationErrorResponse.FieldViolation(
							unrecognized.getPropertyName(), ValidationMessages.UNKNOWN_FIELD)));
			return ResponseEntity.badRequest().body(body);
		}
		Object body = ApiError.of(
				ErrorCode.MALFORMED_REQUEST,
				ErrorCode.MALFORMED_REQUEST.getDefaultMessage(),
				request.getRequestURI());
		return ResponseEntity.status(ErrorCode.MALFORMED_REQUEST.getStatus()).body(body);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
		if (isDuplicateUserEmail(ex)) {
			log.debug("Duplicate email on {} {}", request.getMethod(), request.getRequestURI());
			return build(ErrorCode.EMAIL_ALREADY_EXISTS, request);
		}
		log.warn("Data integrity violation on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return build(ErrorCode.CONCURRENT_MODIFICATION, request);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
		String message = Objects.requireNonNullElse(ex.getReason(), ErrorCode.INTERNAL_ERROR.getDefaultMessage());
		return ResponseEntity.status(ex.getStatusCode())
				.body(ApiError.ofStatus(ex.getStatusCode().value(), message, request.getRequestURI()));
	}

	@ExceptionHandler(ErrorResponseException.class)
	public ResponseEntity<ApiError> handleErrorResponse(ErrorResponseException ex, HttpServletRequest request) {
		var body = ex.getBody();
		String message = body != null && body.getDetail() != null
				? body.getDetail()
				: ErrorCode.INTERNAL_ERROR.getDefaultMessage();
		return ResponseEntity.status(ex.getStatusCode())
				.body(ApiError.ofStatus(ex.getStatusCode().value(), message, request.getRequestURI()));
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
		return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request);
	}

	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<ApiError> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
	                                                             HttpServletRequest request) {
		return build(ErrorCode.NOT_ACCEPTABLE, request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
		return build(ErrorCode.NOT_FOUND, request);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
		return build(ErrorCode.FILE_TOO_LARGE, request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean anonymous = auth == null || auth instanceof AnonymousAuthenticationToken;
		ErrorCode code = anonymous ? ErrorCode.UNAUTHORIZED : ErrorCode.FORBIDDEN;
		return build(code, request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handleOptimisticLock(OptimisticLockingFailureException ex,
	                                                     HttpServletRequest request) {
		log.warn("Optimistic lock failure on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return build(ErrorCode.CONCURRENT_MODIFICATION, request);
	}

	@ExceptionHandler(PessimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handlePessimisticLock(PessimisticLockingFailureException ex,
	                                                      HttpServletRequest request) {
		log.warn("Pessimistic lock failure on {} {}", request.getMethod(), request.getRequestURI(), ex);
		ErrorCode code = ErrorCode.TEMPORARILY_UNAVAILABLE;
		return ResponseEntity.status(code.getStatus())
				.header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
				.body(ApiError.of(code, code.getDefaultMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return build(ErrorCode.INTERNAL_ERROR, request);
	}

	private static String constraintField(ConstraintViolation<?> violation) {
		String path = violation.getPropertyPath().toString();
		int lastDot = path.lastIndexOf('.');
		return lastDot >= 0 ? path.substring(lastDot + 1) : path;
	}

	private static boolean isDuplicateUserEmail(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause().getMessage();
		if (message == null) {
			return false;
		}
		String lower = message.toLowerCase();
		return lower.contains("ux_users_email")
				|| lower.contains("duplicate key")
				|| (lower.contains("users") && lower.contains("email"));
	}

	private ResponseEntity<ApiError> build(ErrorCode code, HttpServletRequest request) {
		return build(code, code.getDefaultMessage(), request);
	}

	private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest request) {
		return ResponseEntity.status(code.getStatus())
				.body(ApiError.of(code, message, request.getRequestURI()));
	}

	private static ResponseEntity<ValidationErrorResponse> validationResponse(
			List<ValidationErrorResponse.FieldViolation> errors) {
		return ResponseEntity.badRequest()
				.body(ValidationErrorResponse.of(ValidationMessages.VALIDATION_FAILED, errors));
	}
}
