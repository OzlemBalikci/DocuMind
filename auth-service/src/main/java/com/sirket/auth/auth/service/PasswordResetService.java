package com.sirket.auth.auth.service;

import com.sirket.auth.auth.dto.request.ForgotPasswordRequest;
import com.sirket.auth.auth.dto.request.ResetPasswordRequest;

public interface PasswordResetService {

	void forgotPassword(ForgotPasswordRequest request, String ipAddress);

	void resetPassword(ResetPasswordRequest request, String ipAddress);
}
