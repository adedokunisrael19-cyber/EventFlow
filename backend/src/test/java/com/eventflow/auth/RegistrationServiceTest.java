package com.eventflow.auth;

import com.eventflow.auth.dto.RegistrationRequest;
import com.eventflow.user.User;
import com.eventflow.user.UserRepository;
import com.eventflow.workspace.Workspace;
import com.eventflow.workspace.WorkspaceMember;
import com.eventflow.workspace.WorkspaceMemberRepository;
import com.eventflow.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private UserRepository userRepository;
    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private EmailVerificationService emailVerificationService;
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        memberRepository = mock(WorkspaceMemberRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailVerificationService = mock(EmailVerificationService.class);
        registrationService = new RegistrationService(
                userRepository,
                workspaceRepository,
                memberRepository,
                passwordEncoder,
                emailVerificationService
        );
    }

    @Test
    void registersAnUnverifiedBusinessOwnerWithAnEncodedPassword() {
        RegistrationRequest request = new RegistrationRequest(
                "  Owner@Example.com ",
                "Strong-password-123",
                "Ada Planner",
                "Ada Events"
        );
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("{bcrypt}encoded-value");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any(WorkspaceMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistrationResponse response = registrationService.register(request);

        assertThat(response.email()).isEqualTo("owner@example.com");
        assertThat(response.fullName()).isEqualTo("Ada Planner");
        assertThat(response.businessName()).isEqualTo("Ada Events");
        assertThat(response.role()).isEqualTo("BUSINESS_OWNER");
        assertThat(response.emailVerificationRequired()).isTrue();
        assertThat(response.userId()).isNotNull();
        assertThat(response.workspaceId()).isNotNull();

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.ArgumentCaptor<Workspace> workspaceCaptor = org.mockito.ArgumentCaptor.forClass(Workspace.class);
        org.mockito.ArgumentCaptor<WorkspaceMember> memberCaptor =
                org.mockito.ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(userRepository).save(userCaptor.capture());
        verify(workspaceRepository).save(workspaceCaptor.capture());
        verify(memberRepository).save(memberCaptor.capture());
        verify(emailVerificationService).issueFor(userCaptor.getValue());
        verify(passwordEncoder).encode(request.password());

        assertThat(userCaptor.getValue().getEmail()).isEqualTo("owner@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("{bcrypt}encoded-value");
        assertThat(userCaptor.getValue().isEmailVerified()).isFalse();
        assertThat(workspaceCaptor.getValue().getCurrencyCode()).isEqualTo("NGN");
        assertThat(workspaceCaptor.getValue().getTimeZone()).isEqualTo("Africa/Lagos");
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(WorkspaceMember.Role.BUSINESS_OWNER);
        assertThat(memberCaptor.getValue().getUser()).isSameAs(userCaptor.getValue());
        assertThat(memberCaptor.getValue().getWorkspace()).isSameAs(workspaceCaptor.getValue());
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegisteredBeforeCreatingWorkspaceData() {
        RegistrationRequest request = new RegistrationRequest(
                "owner@example.com",
                "Strong-password-123",
                "Ada Planner",
                "Ada Events"
        );
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verifyNoInteractions(workspaceRepository, memberRepository, passwordEncoder, emailVerificationService);
        verify(userRepository, never()).save(any(User.class));
    }
}
