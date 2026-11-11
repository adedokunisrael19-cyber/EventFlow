package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import com.eventflow.workspace.Workspace;
import com.eventflow.workspace.WorkspaceMember;
import com.eventflow.workspace.WorkspaceMemberRepository;
import com.eventflow.workspace.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class GoogleAuthenticationService {

    private static final String PROVIDER = "GOOGLE";

    private final GoogleIdentityTokenVerifier tokenVerifier;
    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final AuthenticationService authenticationService;

    public GoogleAuthenticationService(
            GoogleIdentityTokenVerifier tokenVerifier,
            ExternalIdentityRepository identityRepository,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            AuthenticationService authenticationService
    ) {
        this.tokenVerifier = tokenVerifier;
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.authenticationService = authenticationService;
    }

    @Transactional
    public AuthenticationResponse signIn(GoogleSignInRequest request) {
        GoogleIdentityTokenVerifier.GoogleIdentityClaims claims = tokenVerifier.verify(request.idToken())
                .orElseThrow(InvalidCredentialsException::new);
        if (!claims.emailVerified()) {
            throw new InvalidCredentialsException();
        }

        User user = identityRepository.findByProviderAndProviderSubject(PROVIDER, claims.subject())
                .map(ExternalIdentity::getUser)
                .orElseGet(() -> createAccount(claims, request.businessName()));

        return authenticationService.createSession(user);
    }

    private User createAccount(GoogleIdentityTokenVerifier.GoogleIdentityClaims claims, String businessName) {
        String normalizedEmail = claims.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            // Do not automatically link identities based on an email claim. Linking requires
            // proof of control of the existing EventFlow account.
            throw new EmailAlreadyRegisteredException();
        }
        if (businessName == null || businessName.isBlank()) {
            throw new BusinessNameRequiredException();
        }

        User user = userRepository.save(new User(normalizedEmail, claims.fullName().trim(), null));
        user.markEmailVerified();
        Workspace workspace = workspaceRepository.save(new Workspace(businessName.trim()));
        memberRepository.save(new WorkspaceMember(workspace, user, WorkspaceMember.Role.BUSINESS_OWNER));
        identityRepository.save(new ExternalIdentity(user, PROVIDER, claims.subject()));
        return user;
    }
}
