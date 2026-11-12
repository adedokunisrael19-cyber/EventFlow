package com.eventflow.auth;

import com.eventflow.auth.dto.RegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationService authenticationService;
    private final GoogleAuthenticationService googleAuthenticationService;
    private final GooglePasswordSetupService googlePasswordSetupService;

    public AuthController(
            RegistrationService registrationService,
            EmailVerificationService emailVerificationService,
            AuthenticationService authenticationService,
            GoogleAuthenticationService googleAuthenticationService,
            GooglePasswordSetupService googlePasswordSetupService
    ) {
        this.registrationService = registrationService;
        this.emailVerificationService = emailVerificationService;
        this.authenticationService = authenticationService;
        this.googleAuthenticationService = googleAuthenticationService;
        this.googlePasswordSetupService = googlePasswordSetupService;
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

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(authenticationService.login(request.email(), request.password()));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthenticationResponse> googleSignIn(@Valid @RequestBody GoogleSignInRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(googleAuthenticationService.signIn(request));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> setPassword(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @Valid @RequestBody GooglePasswordSetupRequest request
    ) {
        googlePasswordSetupService.setPassword(principal.userId(), request.idToken(), request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return CurrentUserResponse.from(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        authenticationService.logout(principal.sessionId());
        return ResponseEntity.noContent().build();
    }
}
