package com.documind.backend.common.exception;

public class ReauthenticationRequiredException extends ApiException {
	public ReauthenticationRequiredException() {
		super(ErrorCode.REAUTHENTICATION_REQUIRED);
	}
}
