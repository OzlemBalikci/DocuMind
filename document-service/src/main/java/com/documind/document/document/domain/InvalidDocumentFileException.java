package com.documind.document.document.domain;

import com.documind.backend.common.exception.ApiException;
import com.documind.backend.common.exception.ErrorCode;

public class InvalidDocumentFileException extends ApiException {

	public InvalidDocumentFileException(String message) {
		super(ErrorCode.VALIDATION_ERROR, message);
	}
}
