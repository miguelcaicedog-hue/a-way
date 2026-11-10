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
}

interface TeamEntryRepository extends JpaRepository<TeamEntry, UUID> {
    List<TeamEntry> findByCategory_IdOrderByTeam_NameAsc(UUID categoryId);

    Optional<TeamEntry> findByTeam_IdAndCategory_Id(UUID teamId, UUID categoryId);
}

interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findByTeamEntry_IdOrderByCedulaAsc(UUID teamEntryId);

    List<Player> findByCedulaIgnoreCaseOrderByNameAsc(String cedula);
}
