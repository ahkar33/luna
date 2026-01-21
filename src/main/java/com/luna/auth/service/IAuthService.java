package com.luna.auth.service;

import com.luna.auth.dto.AuthRequest;
import com.luna.auth.dto.AuthResponse;
import com.luna.auth.dto.RegisterRequest;

public interface IAuthService {
    
    AuthResponse register(RegisterRequest request);
    
    AuthResponse login(AuthRequest request);
    
    AuthResponse refreshToken(String refreshToken);
}
