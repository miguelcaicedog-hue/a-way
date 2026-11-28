package com.away.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PoliticaClaveRepository extends JpaRepository<PoliticaClave, UUID> {

    Optional<PoliticaClave> findFirstByEstado(String estado);
}
