package com.away.core.competition;

import com.away.core.auth.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LeagueRepository extends JpaRepository<League, UUID> {
    List<League> findAllByOrderByNameAsc();
}

interface LeagueSettingsRepository extends JpaRepository<LeagueSettings, UUID> {
}

interface EditionRepository extends JpaRepository<Edition, UUID> {
    List<Edition> findByLeague_IdOrderByStartsOnDesc(UUID leagueId);
}

interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByEdition_IdOrderByNameAsc(UUID editionId);
}

interface TeamRepository extends JpaRepository<Team, UUID> {
    List<Team> findByLeague_IdOrderByNameAsc(UUID leagueId);

    Optional<Team> findByLeague_IdAndName(UUID leagueId, String name);
}

interface InscripcionArchivoRepository extends JpaRepository<InscripcionArchivo, UUID> {
    List<InscripcionArchivo> findByIdLigaAndIdEdicionAndEstadoOrderByFechaCreacionDesc(UUID idLiga, UUID idEdicion, String estado);
}

interface InscripcionEquipoRepository extends JpaRepository<InscripcionEquipo, UUID> {
    List<InscripcionEquipo> findByIdInscripcionArchivoAndEstadoOrderByNumeroFilaAsc(UUID idInscripcionArchivo, String estado);
}

interface InscripcionJugadorRepository extends JpaRepository<InscripcionJugador, UUID> {
    List<InscripcionJugador> findByIdInscripcionEquipoAndEstadoOrderByOrdenAsc(UUID idInscripcionEquipo, String estado);
}

interface TeamEntryRepository extends JpaRepository<TeamEntry, UUID> {
    List<TeamEntry> findByCategory_IdOrderByTeam_NameAsc(UUID categoryId);

    Optional<TeamEntry> findByTeam_IdAndCategory_Id(UUID teamId, UUID categoryId);
}

interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findByTeamEntry_IdOrderByCedulaAsc(UUID teamEntryId);

    List<Player> findByCedulaIgnoreCaseOrderByNameAsc(String cedula);
}
