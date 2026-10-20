package com.documind.auth.auth.service;

import com.documind.auth.auth.dto.request.ForgotPasswordRequest;
import com.documind.auth.auth.dto.request.ResetPasswordRequest;

public interface PasswordResetService {

	void forgotPassword(ForgotPasswordRequest request, String ipAddress);

	void resetPassword(ResetPasswordRequest request, String ipAddress);
}
