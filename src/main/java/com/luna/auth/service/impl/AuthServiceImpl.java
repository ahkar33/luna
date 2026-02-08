package com.luna.auth.service.impl;

import com.luna.auth.dto.*;
import com.luna.auth.service.GoogleTokenVerifierService;
import com.luna.auth.service.IAuthService;
import com.luna.common.exception.BadRequestException;
import com.luna.common.exception.ResourceNotFoundException;
import com.luna.common.exception.UnauthorizedException;
import com.luna.common.service.EmailService;
import com.luna.common.service.GeoIpService;
import com.luna.user.entity.*;
import com.luna.user.repository.*;
import com.luna.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    @Value("${app.security.device-verification-enabled:true}")
    private boolean deviceVerificationEnabled;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final DeviceVerificationTokenRepository deviceVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final GeoIpService geoIpService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        // Detect country from IP
        String countryCode = null;
        String country = null;
        GeoIpService.GeoIpInfo geoInfo = geoIpService.getGeoInfo(ipAddress);
        if (geoInfo != null) {
            countryCode = geoInfo.countryCode();
            country = geoInfo.country();
        }

        var user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isActive(false)  // Inactive until email verified
                .emailVerified(false)
                .countryCode(countryCode)
                .country(country)
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
        // Check if user exists and is Google-only (no password)
        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent() && userOpt.get().getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Please sign in with Google.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = userOpt
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmailVerified()) {
            throw new UnauthorizedException("Email not verified. Please verify your email first.");
        }

        // Skip device verification if disabled
        if (!deviceVerificationEnabled) {
            var accessToken = jwtService.generateAccessToken(user);
            var refreshToken = createRefreshToken(user);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .requiresDeviceVerification(false)
                    .build();
        }

        // Check if user has any verified devices (to determine if this is first login)
        long verifiedDeviceCount = userDeviceRepository.countByUserIdAndVerified(user.getId(), true);
        boolean isFirstLogin = verifiedDeviceCount == 0;

        // Check if current device is known and verified
        var deviceOpt = userDeviceRepository.findByUserIdAndDeviceFingerprint(user.getId(), deviceFingerprint);
        
        // If first login, auto-verify the device and proceed
        if (isFirstLogin) {
            UserDevice device;
            if (deviceOpt.isPresent()) {
                device = deviceOpt.get();
                device.setVerified(true);
                device.setVerifiedAt(Instant.now());
                device.setLastSeenAt(Instant.now());
                device.setIpAddress(ipAddress);
            } else {
                device = UserDevice.builder()
                        .user(user)
                        .deviceFingerprint(deviceFingerprint)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .verified(true)
                        .verifiedAt(Instant.now())
                        .build();
            }
            userDeviceRepository.save(device);

            var accessToken = jwtService.generateAccessToken(user);
            var refreshToken = createRefreshToken(user);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .requiresDeviceVerification(false)
                    .build();
        }
        
        // Not first login - check if device is verified
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
        var oldRefreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (oldRefreshToken.isRevoked() || oldRefreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        // Revoke old refresh token
        oldRefreshToken.setRevoked(true);
        refreshTokenRepository.save(oldRefreshToken);

        // Generate new tokens
        var user = oldRefreshToken.getUser();
        var accessToken = jwtService.generateAccessToken(user);
        var newRefreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken.getToken())
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var verificationToken = verificationTokenRepository.findByOtpAndUserId(request.getOtp(), user.getId())
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (verificationToken.getUsed()) {
            throw new BadRequestException("OTP already used");
        }

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        // Rate limiting: Check if there's a recent OTP (within last 60 seconds)
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        var recentToken = verificationTokenRepository
                .findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), oneMinuteAgo);
        
        if (recentToken.isPresent()) {
            long secondsLeft = 60 - (Instant.now().getEpochSecond() - recentToken.get().getCreatedAt().getEpochSecond());
            throw new BadRequestException("Please wait " + secondsLeft + " seconds before requesting a new code");
        }

        // Generate and send new OTP
        String otp = generateOtp();
        createVerificationToken(user, otp);
        emailService.sendVerificationEmail(user.getEmail(), otp);
    }

    @Override
    @Transactional
    public void resendDeviceOtp(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmailVerified()) {
            throw new BadRequestException("Please verify your email first");
        }

        // Find the most recent device verification token to get device fingerprint
        var recentDeviceToken = deviceVerificationTokenRepository
                .findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new BadRequestException("No pending device verification found"));

        String deviceFingerprint = recentDeviceToken.getDeviceFingerprint();

        // Generate and send new device OTP
        String otp = generateOtp();
        createDeviceVerificationToken(user, deviceFingerprint, otp);
        emailService.sendDeviceVerificationEmail(user.getEmail(), deviceFingerprint, otp);
    }

    private String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        int otp = 100000 + secureRandom.nextInt(900000);
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var deviceToken = deviceVerificationTokenRepository
                .findByOtpAndUserIdAndDeviceFingerprint(request.getOtp(), user.getId(), request.getDeviceFingerprint())
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (deviceToken.getUsed()) {
            throw new BadRequestException("OTP already used");
        }

        if (deviceToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
        }

        // Mark device as verified
        var device = userDeviceRepository
                .findByUserIdAndDeviceFingerprint(user.getId(), request.getDeviceFingerprint())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
        
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

    @Override
    @Transactional
    public void forgotPassword(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPassword() == null) {
            throw new BadRequestException("This account uses Google Sign-In. Password reset is not available.");
        }

        // Rate limiting: Check if there's a recent OTP (within last 60 seconds)
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);
        var recentToken = passwordResetTokenRepository
                .findFirstByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), oneMinuteAgo);
        
        if (recentToken.isPresent()) {
            long secondsLeft = 60 - (Instant.now().getEpochSecond() - recentToken.get().getCreatedAt().getEpochSecond());
            throw new BadRequestException("Please wait " + secondsLeft + " seconds before requesting a new code");
        }

        // Generate and send OTP
        String otp = generateOtp();
        var token = PasswordResetToken.builder()
                .otp(otp)
                .user(user)
                .expiryDate(Instant.now().plusSeconds(900)) // 15 minutes
                .used(false)
                .build();
        passwordResetTokenRepository.save(token);

        emailService.sendPasswordResetEmail(user.getEmail(), otp);
    }

    @Override
    @Transactional
    public void verifyResetPasswordOtp(VerifyResetPasswordOtpRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var resetToken = passwordResetTokenRepository.findByOtpAndUserId(request.getOtp(), user.getId())
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (resetToken.getUsed()) {
            throw new BadRequestException("OTP already used");
        }

        if (resetToken.getVerified()) {
            throw new BadRequestException("OTP already verified");
        }

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
        }

        // Mark token as verified
        resetToken.setVerified(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find the verified token
        var resetToken = passwordResetTokenRepository
                .findFirstByUserIdAndVerifiedTrueAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new BadRequestException("No verified OTP found. Please verify your OTP first."));

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired. Please request a new password reset.");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request, String ipAddress) {
        var payload = googleTokenVerifierService.verify(request.getIdToken());

        String email = payload.getEmail();
        String googleUserId = payload.getSubject();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        if (email == null || !payload.getEmailVerified()) {
            throw new BadRequestException("Google account email is not verified");
        }

        // 1. Look up by Google provider ID
        var existingGoogleUser = userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, googleUserId);
        if (existingGoogleUser.isPresent()) {
            var user = existingGoogleUser.get();
            var accessToken = jwtService.generateAccessToken(user);
            var refreshToken = createRefreshToken(user);
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .build();
        }

        // 2. Look up by email (auto-link existing LOCAL account)
        var existingEmailUser = userRepository.findByEmail(email);
        if (existingEmailUser.isPresent()) {
            var user = existingEmailUser.get();
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setProviderId(googleUserId);
            user.setEmailVerified(true);
            user.setIsActive(true);
            if (user.getProfileImageUrl() == null && pictureUrl != null) {
                user.setProfileImageUrl(pictureUrl);
            }
            userRepository.save(user);

            var accessToken = jwtService.generateAccessToken(user);
            var refreshToken = createRefreshToken(user);
            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .build();
        }

        // 3. Create new user
        String username = generateUniqueUsername(email, name);

        // Detect country from IP
        String countryCode = null;
        String country = null;
        GeoIpService.GeoIpInfo geoInfo = geoIpService.getGeoInfo(ipAddress);
        if (geoInfo != null) {
            countryCode = geoInfo.countryCode();
            country = geoInfo.country();
        }

        var newUser = User.builder()
                .email(email)
                .username(username)
                .password(null)
                .role(Role.USER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(googleUserId)
                .isActive(true)
                .emailVerified(true)
                .profileImageUrl(pictureUrl)
                .countryCode(countryCode)
                .country(country)
                .build();
        userRepository.save(newUser);

        var accessToken = jwtService.generateAccessToken(newUser);
        var refreshToken = createRefreshToken(newUser);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    private String generateUniqueUsername(String email, String name) {
        // Try name first (lowercase, no spaces), then email prefix
        String base = name != null ? name.toLowerCase().replaceAll("[^a-z0-9]", "") : email.split("@")[0];
        if (base.length() > 20) {
            base = base.substring(0, 20);
        }
        if (base.length() < 3) {
            base = "user" + base;
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffixStr = String.valueOf(suffix);
            int maxBase = 20 - suffixStr.length();
            candidate = base.substring(0, Math.min(base.length(), maxBase)) + suffixStr;
            suffix++;
        }
        return candidate;
    }
}
