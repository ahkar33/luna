package com.luna.auth.service.impl;

import com.luna.auth.dto.*;
import com.luna.auth.service.IAuthService;
import com.luna.common.service.EmailService;
import com.luna.user.entity.*;
import com.luna.user.repository.*;
import com.luna.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final DeviceVerificationTokenRepository deviceVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        var user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isActive(false)  // Inactive until email verified
                .emailVerified(false)
                .build();

        userRepository.save(user);

        // Generate and send OTP
        String otp = generateOtp();
        createVerificationToken(user, otp);
        emailService.sendVerificationEmail(user.getEmail(), otp);

        return AuthResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .build();
    }

    @Override
    @Transactional
    public LoginResponse login(AuthRequest request, String deviceFingerprint, String ipAddress, String userAgent) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmailVerified()) {
            throw new RuntimeException("Email not verified. Please verify your email first.");
        }

        // Check if device is known and verified
        var deviceOpt = userDeviceRepository.findByUserIdAndDeviceFingerprint(user.getId(), deviceFingerprint);
        
        if (deviceOpt.isEmpty() || !deviceOpt.get().getVerified()) {
            // New device or unverified device - send OTP
            String otp = generateOtp();
            createDeviceVerificationToken(user, deviceFingerprint, otp);
            
            String deviceInfo = String.format("IP: %s\nBrowser: %s", ipAddress, userAgent);
            emailService.sendDeviceVerificationEmail(user.getEmail(), otp, deviceInfo);
            
            // Save device info if new
            if (deviceOpt.isEmpty()) {
                var newDevice = UserDevice.builder()
                        .user(user)
                        .deviceFingerprint(deviceFingerprint)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .verified(false)
                        .build();
                userDeviceRepository.save(newDevice);
            }
            
            return LoginResponse.builder()
                    .requiresDeviceVerification(true)
                    .message("New device detected. Please check your email for verification code.")
                    .build();
        }

        // Known and verified device - proceed with login
        var device = deviceOpt.get();
        device.setLastSeenAt(Instant.now());
        device.setIpAddress(ipAddress);
        userDeviceRepository.save(device);

        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = createRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .requiresDeviceVerification(false)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        var refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        var accessToken = jwtService.generateAccessToken(refreshToken.getUser());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        var refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(604800000))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var verificationToken = verificationTokenRepository.findByOtpAndUserId(request.getOtp(), user.getId())
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (verificationToken.getUsed()) {
            throw new RuntimeException("OTP already used");
        }

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("OTP expired");
        }

        // Mark user as verified
        user.setEmailVerified(true);
        user.setIsActive(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }

    @Override
    @Transactional
    public void resendOtp(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        // Rate limiting: Check if there's a recent OTP (within last 60 seconds)
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        var recentToken = verificationTokenRepository
                .findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), oneMinuteAgo);
        
        if (recentToken.isPresent()) {
            long secondsLeft = 60 - (Instant.now().getEpochSecond() - recentToken.get().getCreatedAt().getEpochSecond());
            throw new RuntimeException("Please wait " + secondsLeft + " seconds before requesting a new code");
        }

        // Generate and send new OTP
        String otp = generateOtp();
        createVerificationToken(user, otp);
        emailService.sendVerificationEmail(user.getEmail(), otp);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private void createVerificationToken(User user, String otp) {
        var token = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .otp(otp)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(900)) // 15 minutes
                .used(false)
                .build();

        verificationTokenRepository.save(token);
    }

    @Override
    @Transactional
    public AuthResponse verifyDevice(VerifyDeviceRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var deviceToken = deviceVerificationTokenRepository
                .findByOtpAndUserIdAndDeviceFingerprint(request.getOtp(), user.getId(), request.getDeviceFingerprint())
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (deviceToken.getUsed()) {
            throw new RuntimeException("OTP already used");
        }

        if (deviceToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("OTP expired");
        }

        // Mark device as verified
        var device = userDeviceRepository
                .findByUserIdAndDeviceFingerprint(user.getId(), request.getDeviceFingerprint())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        
        device.setVerified(true);
        device.setVerifiedAt(Instant.now());
        userDeviceRepository.save(device);

        // Mark token as used
        deviceToken.setUsed(true);
        deviceVerificationTokenRepository.save(deviceToken);

        // Generate tokens
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    private void createDeviceVerificationToken(User user, String deviceFingerprint, String otp) {
        var token = DeviceVerificationToken.builder()
                .otp(otp)
                .user(user)
                .deviceFingerprint(deviceFingerprint)
                .expiryDate(Instant.now().plusSeconds(600)) // 10 minutes
                .used(false)
                .build();

        deviceVerificationTokenRepository.save(token);
    }
}
