package com.documind.user.common;

import com.documind.platform.exception.ApiException;
import com.documind.platform.exception.ErrorCode;

public class AccountDeletionPendingException extends ApiException {

	public AccountDeletionPendingException() {
		super(ErrorCode.ACCOUNT_DELETION_PENDING);
	}
}
