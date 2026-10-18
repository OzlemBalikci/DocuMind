package com.documind.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request body"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or has expired"),
    INVALID_PASSWORD_RESET_CODE(HttpStatus.BAD_REQUEST, "Invalid or expired password reset code"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to access this resource"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this endpoint"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file exceeds the maximum allowed size"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The uploaded file type is not supported"),
    QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Usage quota exceeded, please try again later"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests, please try again later"),
    QUEUE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The service is temporarily unavailable, please try again later"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request content type is not supported"),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "The requested response format is not available"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "The resource was modified by another request, please retry"),
    TEMPORARILY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "The service is temporarily unavailable, please retry shortly");

    private final HttpStatus status;
    private final String defaultMessage;
}
