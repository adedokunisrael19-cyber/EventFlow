package com.eventflow.auth;

import com.eventflow.auth.dto.RegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService
    ) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
    }

    @PostMapping("/verify-email")
    public EmailVerificationResponse verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        emailVerificationService.verify(request.token());
        return new EmailVerificationResponse("Email address verified successfully.");
    }
}
