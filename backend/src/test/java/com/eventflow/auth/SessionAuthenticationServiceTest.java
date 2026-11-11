package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.workspace.Workspace;
import com.eventflow.workspace.WorkspaceMember;
import com.eventflow.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionAuthenticationServiceTest {

    private UserSessionRepository sessionRepository;
    private WorkspaceMemberRepository memberRepository;
    private SessionAuthenticationService service;
    private final Instant now = Instant.parse("2026-11-11T12:00:00Z");

    @BeforeEach
    void setUp() {
        sessionRepository = mock(UserSessionRepository.class);
        memberRepository = mock(WorkspaceMemberRepository.class);
        service = new SessionAuthenticationService(
                sessionRepository,
                memberRepository,
                Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    @Test
    void resolvesAnActiveBearerTokenToItsWorkspaceMembership() {
        User user = new User("owner@example.com", "Ada Planner", "encoded-password");
        UserSession session = new UserSession(user, "stored-hash", now.plusSeconds(60));
        Workspace workspace = new Workspace("Ada Events");
        WorkspaceMember member = new WorkspaceMember(workspace, user, WorkspaceMember.Role.BUSINESS_OWNER);
        when(sessionRepository.findByTokenHash(any())).thenReturn(Optional.of(session));
        when(memberRepository.findByUser_Id(user.getId())).thenReturn(Optional.of(member));

        Optional<CurrentUserPrincipal> principal = service.authenticate("opaque-bearer-token");

        assertThat(principal).isPresent();
        assertThat(principal.get().userId()).isEqualTo(user.getId());
        assertThat(principal.get().workspaceId()).isEqualTo(workspace.getId());
        assertThat(principal.get().workspaceName()).isEqualTo("Ada Events");
        assertThat(principal.get().role()).isEqualTo("BUSINESS_OWNER");
        var hashCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sessionRepository).findByTokenHash(hashCaptor.capture());
        assertThat(hashCaptor.getValue()).hasSize(64).isNotEqualTo("opaque-bearer-token");
    }

    @Test
    void ignoresExpiredSessions() {
        User user = new User("owner@example.com", "Ada Planner", "encoded-password");
        when(sessionRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(new UserSession(user, "stored-hash", now)));

        assertThat(service.authenticate("expired-token")).isEmpty();
    }

    @Test
    void ignoresSessionsWithoutAWorkspaceMembership() {
        User user = new User("owner@example.com", "Ada Planner", "encoded-password");
        when(sessionRepository.findByTokenHash(any()))
                .thenReturn(Optional.of(new UserSession(user, "stored-hash", now.plusSeconds(60))));
        when(memberRepository.findByUser_Id(user.getId())).thenReturn(Optional.empty());

        assertThat(service.authenticate("orphaned-token")).isEmpty();
    }
}
