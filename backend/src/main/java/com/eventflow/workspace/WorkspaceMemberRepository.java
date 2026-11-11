package com.eventflow.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    Optional<WorkspaceMember> findByUser_Id(UUID userId);
}
