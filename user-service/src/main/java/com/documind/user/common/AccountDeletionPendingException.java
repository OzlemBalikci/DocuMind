package com.documind.user.common;

import com.documind.backend.common.exception.ApiException;
import com.documind.backend.common.exception.ErrorCode;

public class AccountDeletionPendingException extends ApiException {

	public AccountDeletionPendingException() {
		super(ErrorCode.ACCOUNT_DELETION_PENDING);
	}
}
