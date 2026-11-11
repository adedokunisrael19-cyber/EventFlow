package com.eventflow.auth;

import com.eventflow.auth.dto.RegistrationRequest;
import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import com.eventflow.workspace.Workspace;
import com.eventflow.workspace.WorkspaceMember;
import com.eventflow.workspace.WorkspaceMemberRepository;
import com.eventflow.workspace.WorkspaceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public RegistrationService(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = userRepository.save(new User(
                normalizedEmail,
                request.fullName().trim(),
                passwordEncoder.encode(request.password())
        ));

        Workspace workspace = workspaceRepository.save(new Workspace(request.businessName().trim()));
        memberRepository.save(new WorkspaceMember(workspace, user, WorkspaceMember.Role.BUSINESS_OWNER));
        emailVerificationService.issueFor(user);

        return new RegistrationResponse(
                user.getId(),
                workspace.getId(),
                user.getEmail(),
                user.getFullName(),
                workspace.getBusinessName(),
                WorkspaceMember.Role.BUSINESS_OWNER.name(),
                true
        );
    }
}
