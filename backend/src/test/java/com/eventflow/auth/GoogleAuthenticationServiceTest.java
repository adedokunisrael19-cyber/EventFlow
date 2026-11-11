package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import com.eventflow.workspace.Workspace;
import com.eventflow.workspace.WorkspaceMember;
import com.eventflow.workspace.WorkspaceMemberRepository;
import com.eventflow.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAuthenticationServiceTest {

    private GoogleIdentityTokenVerifier tokenVerifier;
    private ExternalIdentityRepository identityRepository;
    private UserRepository userRepository;
    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository memberRepository;
    private AuthenticationService authenticationService;
    private GoogleAuthenticationService service;

    @BeforeEach
    void setUp() {
        tokenVerifier = mock(GoogleIdentityTokenVerifier.class);
        identityRepository = mock(ExternalIdentityRepository.class);
        userRepository = mock(UserRepository.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        memberRepository = mock(WorkspaceMemberRepository.class);
        authenticationService = mock(AuthenticationService.class);
        service = new GoogleAuthenticationService(tokenVerifier, identityRepository, userRepository,
                workspaceRepository, memberRepository, authenticationService);
    }

    @Test
    void returningGoogleIdentityGetsAnEventFlowSessionWithoutCreatingAnotherWorkspace() {
        User user = new User("owner@example.com", "Ada Planner", null);
        when(tokenVerifier.verify("signed-id-token")).thenReturn(Optional.of(
                new GoogleIdentityTokenVerifier.GoogleIdentityClaims("google-sub-123", "owner@example.com", "Ada Planner", true)));
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(new ExternalIdentity(user, "GOOGLE", "google-sub-123")));
        AuthenticationResponse expected = new AuthenticationResponse("opaque-token", "Bearer", Instant.now());
        when(authenticationService.createSession(user)).thenReturn(expected);

        AuthenticationResponse actual = service.signIn(new GoogleSignInRequest("signed-id-token", null));

        assertThat(actual).isSameAs(expected);
        verify(authenticationService).createSession(user);
        verify(userRepository, never()).save(any(User.class));
        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    void newGoogleIdentityNeedsBusinessNameBeforeCreatingAnAccount() {
        when(tokenVerifier.verify("signed-id-token")).thenReturn(Optional.of(
                new GoogleIdentityTokenVerifier.GoogleIdentityClaims("google-sub-123", "owner@example.com", "Ada Planner", true)));
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.signIn(new GoogleSignInRequest("signed-id-token", " ")))
                .isInstanceOf(BusinessNameRequiredException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    void rejectsEmailCollisionInsteadOfLinkingToExistingPasswordAccount() {
        when(tokenVerifier.verify("signed-id-token")).thenReturn(Optional.of(
                new GoogleIdentityTokenVerifier.GoogleIdentityClaims("google-sub-123", "owner@example.com", "Ada Planner", true)));
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.signIn(new GoogleSignInRequest("signed-id-token", "Ada Events")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(authenticationService, never()).createSession(any(User.class));
    }

    @Test
    void rejectsAnUnverifiedGoogleEmailClaim() {
        when(tokenVerifier.verify("signed-id-token")).thenReturn(Optional.of(
                new GoogleIdentityTokenVerifier.GoogleIdentityClaims("google-sub-123", "owner@example.com", "Ada Planner", false)));
        when(identityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.signIn(new GoogleSignInRequest("signed-id-token", "Ada Events")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
