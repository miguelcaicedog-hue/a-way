package com.away.core.competition;

import com.away.core.auth.League;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CompetitionController {

    private final CompetitionService competition;

    public CompetitionController(CompetitionService competition) {
        this.competition = competition;
    }

    @GetMapping("/leagues")
    public List<LeagueView> leagues() {
        return competition.listLeagues();
    }

    @PostMapping("/leagues")
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueView createLeague(@Valid @RequestBody LeagueRequest request) {
        return competition.createLeague(request);
    }

    @PutMapping("/leagues/{id}")
    public LeagueView updateLeague(@PathVariable UUID id, @Valid @RequestBody LeagueRequest request) {
        return competition.updateLeague(id, request);
    }

    @DeleteMapping("/leagues/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLeague(@PathVariable UUID id) {
        competition.deleteLeague(id);
    }

    @GetMapping("/leagues/{id}/logo")
    public ResponseEntity<byte[]> leagueLogo(@PathVariable UUID id) {
        League league = competition.leagueLogo(id);
        if (league.getLogo() == null || league.getLogoContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(league.getLogoContentType()))
                .body(league.getLogo());
    }

    @GetMapping("/editions")
    public List<EditionView> editions(@RequestParam UUID leagueId) {
        return competition.listEditions(leagueId);
    }

    @PostMapping("/editions")
    @ResponseStatus(HttpStatus.CREATED)
    public EditionView createEdition(@Valid @RequestBody EditionRequest request) {
        return competition.createEdition(request);
    }

    @PutMapping("/editions/{id}")
    public EditionView updateEdition(@PathVariable UUID id, @Valid @RequestBody EditionRequest request) {
        return competition.updateEdition(id, request);
    }

    @DeleteMapping("/editions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEdition(@PathVariable UUID id) {
        competition.deleteEdition(id);
    }

    @GetMapping("/categories")
    public List<CategoryView> categories(@RequestParam UUID editionId) {
        return competition.listCategories(editionId);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView createCategory(@Valid @RequestBody CategoryRequest request) {
        return competition.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryView updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return competition.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id) {
        competition.deleteCategory(id);
    }

    @GetMapping("/teams")
    public List<TeamView> teams(@RequestParam UUID leagueId,
                                @RequestParam(required = false) String nombre,
                                @RequestParam(required = false) String representante,
                                @RequestParam(required = false) String estado,
                                @RequestParam(required = false) UUID editionId) {
        return competition.listTeams(leagueId, nombre, representante, estado, editionId);
    }

    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamView createTeam(@Valid @RequestBody TeamRequest request) {
        return competition.createTeam(request);
    }

    @PutMapping("/teams/{id}")
    public TeamView updateTeam(@PathVariable UUID id, @Valid @RequestBody TeamRequest request) {
        return competition.updateTeam(id, request);
    }

    @DeleteMapping("/teams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable UUID id) {
        competition.deleteTeam(id);
    }

    @GetMapping("/teams/{id}/logo")
    public ResponseEntity<byte[]> teamLogo(@PathVariable UUID id) {
        Team team = competition.teamLogo(id);
        if (team.getLogo() == null || team.getLogoContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(team.getLogoContentType()))
                .body(team.getLogo());
    }

    @GetMapping("/team-entries")
    public List<TeamEntryView> teamEntries(@RequestParam UUID categoryId) {
        return competition.listTeamEntries(categoryId);
    }

    @PostMapping("/team-entries")
    @ResponseStatus(HttpStatus.CREATED)
    public TeamEntryView createTeamEntry(@Valid @RequestBody TeamEntryRequest request) {
        return competition.createTeamEntry(request);
    }

    @GetMapping("/players")
    public List<PlayerView> players(@RequestParam UUID teamEntryId) {
        return competition.listPlayers(teamEntryId);
    }

    @GetMapping("/players/lookup")
    public ResponseEntity<PlayerView> lookupPlayer(@RequestParam String cedula) {
        PlayerView player = competition.findByCedula(cedula);
        if (player == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(player);
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    public PlayerView createPlayer(@Valid @RequestBody PlayerRequest request) {
        return competition.createPlayer(request);
    }

    @PutMapping("/players/{id}")
    public PlayerView updatePlayer(@PathVariable UUID id, @Valid @RequestBody PlayerRequest request) {
        return competition.updatePlayer(id, request);
    }

    @DeleteMapping("/players/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable UUID id) {
        competition.deletePlayer(id);
    }

    @GetMapping("/players/{id}/photo")
    public ResponseEntity<byte[]> playerPhoto(@PathVariable UUID id) {
        Player player = competition.playerPhoto(id);
        if (player.getPhoto() == null || player.getPhotoContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(player.getPhotoContentType()))
                .body(player.getPhoto());
    }
}
