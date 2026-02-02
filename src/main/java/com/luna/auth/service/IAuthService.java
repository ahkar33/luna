package com.luna.auth.service;

import com.luna.auth.dto.*;

public interface IAuthService {
    
    AuthResponse register(RegisterRequest request, String ipAddress);
    
    LoginResponse login(AuthRequest request, String deviceFingerprint, String ipAddress, String userAgent);
    
    AuthResponse refreshToken(String refreshToken);
    
    void verifyEmail(VerifyEmailRequest request);
    
    void resendOtp(String email);
    
    void resendDeviceOtp(String email);
    
    AuthResponse verifyDevice(VerifyDeviceRequest request);
    
    void forgotPassword(String email);

    void verifyResetPasswordOtp(VerifyResetPasswordOtpRequest request);

    void resetPassword(ResetPasswordRequest request);
}
