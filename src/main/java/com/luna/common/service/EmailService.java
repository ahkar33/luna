package com.luna.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Luna - Email Verification");
            message.setText(String.format(
                "Welcome to Luna!\n\n" +
                "Your verification code is: %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't create an account, please ignore this email.",
                otp
            ));
            
            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }

    public void sendDeviceVerificationEmail(String to, String otp, String deviceInfo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Luna - New Device Login Detected");
            message.setText(String.format(
                "New Device Login Detected\n\n" +
                "We detected a login from a new device:\n%s\n\n" +
                "Your verification code is: %s\n\n" +
                "This code will expire in 10 minutes.\n\n" +
                "If this wasn't you, please change your password immediately.",
                deviceInfo,
                otp
            ));
            
            mailSender.send(message);
            log.info("Device verification email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send device verification email to: {}", to, e);
            throw new RuntimeException("Failed to send device verification email");
        }
    }

    public void sendPasswordResetEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Luna - Password Reset");
            message.setText(String.format(
                "Password Reset Request\n\n" +
                "Your password reset code is: %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't request a password reset, please ignore this email.",
                otp
            ));
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }
}
