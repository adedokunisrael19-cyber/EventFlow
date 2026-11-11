package com.eventflow.auth;

import com.eventflow.user.User;
import com.eventflow.workspace.WorkspaceMember;
import com.eventflow.workspace.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class SessionAuthenticationService {

    private final UserSessionRepository sessionRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final Clock clock;

    public SessionAuthenticationService(
            UserSessionRepository sessionRepository,
            WorkspaceMemberRepository memberRepository,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<CurrentUserPrincipal> authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        return sessionRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .filter(session -> session.isActiveAt(clock.instant()))
                .flatMap(session -> {
                    User user = session.getUser();
                    return memberRepository.findByUser_Id(user.getId())
                            .map(member -> principalFor(session, user, member));
                });
    }

    private CurrentUserPrincipal principalFor(UserSession session, User user, WorkspaceMember member) {
        return new CurrentUserPrincipal(
                user.getId(),
                session.getId(),
                user.getEmail(),
                user.getFullName(),
                member.getWorkspace().getId(),
                member.getWorkspace().getBusinessName(),
                member.getRole().name()
        );
    }
}
