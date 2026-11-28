package com.away.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsuarioLeagueRepository extends JpaRepository<League, UUID> {
}
