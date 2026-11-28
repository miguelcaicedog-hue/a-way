package com.away.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeagueMembershipRepository extends JpaRepository<LeagueMembership, UUID> {

    Optional<LeagueMembership> findByUser_IdAndLeague_Id(UUID userId, UUID leagueId);
}
