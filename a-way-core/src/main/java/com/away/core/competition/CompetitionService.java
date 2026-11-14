package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import com.away.core.auth.LeagueMembership;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompetitionService {

    private final AppUserRepository users;
    private final LeagueRepository leagues;
    private final LeagueSettingsRepository settings;
    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final TeamRepository teams;
    private final TeamEntryRepository entries;
    private final PlayerRepository players;
    private final SuspensionesService suspensiones;
    private final CriterioDesempateGoleadorService criteriosGoleador;
    private final CriterioDesempateEquipoService criteriosEquipo;

    public CompetitionService(AppUserRepository users,
                              LeagueRepository leagues,
                              LeagueSettingsRepository settings,
                              EditionRepository editions,
                              CategoryRepository categories,
                              TeamRepository teams,
                              TeamEntryRepository entries,
                              PlayerRepository players,
                              SuspensionesService suspensiones,
                              CriterioDesempateGoleadorService criteriosGoleador,
                              CriterioDesempateEquipoService criteriosEquipo) {
        this.users = users;
        this.leagues = leagues;
        this.settings = settings;
        this.editions = editions;
        this.categories = categories;
        this.teams = teams;
        this.entries = entries;
        this.players = players;
        this.suspensiones = suspensiones;
        this.criteriosGoleador = criteriosGoleador;
        this.criteriosEquipo = criteriosEquipo;
    }

    @Transactional(readOnly = true)
    public List<LeagueView> listLeagues() {
        AppUser user = currentUser();
        if (user.isMaster()) {
            return leagues.findAllByOrderByNameAsc().stream().map(this::leagueView).collect(Collectors.toList());
        }
        return user.getMemberships().stream()
                .map(LeagueMembership::getLeague)
                .map(this::leagueView)
                .collect(Collectors.toList());
    }

    public LeagueView createLeague(LeagueRequest request) {
        if (!currentUser().isMaster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el master puede crear ligas");
        }
        assertDateOrder(request.getValidFrom(), request.getValidUntil(), "La vigencia final no puede ser anterior a la inicial");
        League league = new League();
        league.setId(UUID.randomUUID());
        applyLeague(league, request);
        leagues.save(league);
        LeagueSettings defaults = new LeagueSettings();
        defaults.setLeagueId(league.getId());
        defaults.setSuspensionTiming("NEXT_MATCHDAY");
        defaults.setFairPlayYellowPoints(1);
        defaults.setFairPlayRedPoints(3);
        settings.save(defaults);
        suspensiones.crearDefecto(league.getId());
        return leagueView(league);
    }

    public LeagueView updateLeague(UUID id, LeagueRequest request) {
        League league = league(id);
        assertWrite(league.getId());
        assertDateOrder(request.getValidFrom(), request.getValidUntil(), "La vigencia final no puede ser anterior a la inicial");
        applyLeague(league, request);
        return leagueView(league);
    }

    public void deleteLeague(UUID id) {
        if (!currentUser().isMaster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el master puede eliminar ligas");
        }
        League league = league(id);
        settings.deleteById(league.getId());
        leagues.delete(league);
    }

    @Transactional(readOnly = true)
    public List<EditionView> listEditions(UUID leagueId) {
        assertRead(leagueId);
        return editions.findByLeague_IdOrderByStartsOnDesc(leagueId).stream()
                .map(this::editionView)
                .collect(Collectors.toList());
    }

    public EditionView createEdition(EditionRequest request) {
        League league = league(request.getLeagueId());
        assertWrite(league.getId());
        assertDateOrder(request.getStartsOn(), request.getEndsOn(), "La fecha final no puede ser anterior a la inicial");
        Edition edition = new Edition();
        edition.setId(UUID.randomUUID());
        edition.setLeague(league);
        edition.setName(request.getName().trim());
        edition.setStartsOn(request.getStartsOn());
        edition.setEndsOn(request.getEndsOn());
        Edition guardada = editions.save(edition);
        criteriosGoleador.sembrar(guardada);
        criteriosEquipo.sembrar(guardada);
        return editionView(guardada);
    }

    public EditionView updateEdition(UUID id, EditionRequest request) {
        Edition edition = edition(id);
        League league = league(request.getLeagueId());
        assertWrite(edition.getLeague().getId());
        assertWrite(league.getId());
        assertDateOrder(request.getStartsOn(), request.getEndsOn(), "La fecha final no puede ser anterior a la inicial");
        edition.setLeague(league);
        edition.setName(request.getName().trim());
        edition.setStartsOn(request.getStartsOn());
        edition.setEndsOn(request.getEndsOn());
        return editionView(edition);
    }

    public void deleteEdition(UUID id) {
        Edition edition = edition(id);
        assertWrite(edition.getLeague().getId());
        editions.delete(edition);
    }

    @Transactional(readOnly = true)
    public List<CategoryView> listCategories(UUID editionId, String estado) {
        Edition edition = edition(editionId);
        assertRead(edition.getLeague().getId());
        return categoriasDe(editionId, estado).stream()
                .map(this::categoryView)
                .collect(Collectors.toList());
    }

    public CategoryView createCategory(CategoryRequest request) {
        Edition edition = edition(request.getEditionId());
        assertWrite(edition.getLeague().getId());
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setEdition(edition);
        category.setName(request.getName().trim());
        category.setMaxPlayers(request.getMaxPlayers());
        category.setCantidadGrupos(request.getCantidadGrupos());
        category.setEquiposPorGrupo(request.getEquiposPorGrupo());
        category.setCantidadFases(request.getCantidadFases());
        category.setEstado(estadoCategoria(request.getEstado(), "ACTIVO"));
        return categoryView(categories.save(category));
    }

    public CategoryView updateCategory(UUID id, CategoryRequest request) {
        Category category = category(id);
        Edition edition = edition(request.getEditionId());
        assertWrite(category.getEdition().getLeague().getId());
        assertWrite(edition.getLeague().getId());
        category.setEdition(edition);
        category.setName(request.getName().trim());
        category.setMaxPlayers(request.getMaxPlayers());
        category.setCantidadGrupos(request.getCantidadGrupos());
        category.setEquiposPorGrupo(request.getEquiposPorGrupo());
        category.setCantidadFases(request.getCantidadFases());
        if (request.getEstado() != null && !request.getEstado().isBlank()) {
            category.setEstado(estadoCategoria(request.getEstado(), category.getEstado()));
        }
        return categoryView(category);
    }

    public void deleteCategory(UUID id) {
        Category category = category(id);
        assertWrite(category.getEdition().getLeague().getId());
        category.setEstado("INACTIVO");
        categories.save(category);
    }

    @Transactional(readOnly = true)
    public List<TeamView> listTeams(UUID leagueId, String nombre, String representante, String estado, UUID editionId, UUID categoryId) {
        assertRead(leagueId);
        String filtroEstado = estado == null ? "ACTIVO" : estado.trim();
        if (!filtroEstado.isEmpty()) {
            filtroEstado = estadoEquipo(filtroEstado);
        }
        List<Team> encontrados = teams.buscar(leagueId, textoFiltro(nombre), textoFiltro(representante), filtroEstado);
        if (categoryId != null) {
            Category categoria = category(categoryId);
            if (!leagueId.equals(categoria.getEdition().getLeague().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a la liga");
            }
            if (editionId != null && !editionId.equals(categoria.getEdition().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a la edición");
            }
            if (editionId == null) {
                editionId = categoria.getEdition().getId();
            }
            java.util.Set<UUID> inscritos = entries.findByCategory_IdOrderByTeam_NameAsc(categoryId).stream()
                    .map(entry -> entry.getTeam().getId())
                    .collect(Collectors.toSet());
            encontrados = encontrados.stream()
                    .filter(team -> inscritos.contains(team.getId()))
                    .collect(Collectors.toList());
        }
        if (editionId != null) {
            Edition edition = editions.findById(editionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La edición no existe"));
            if (!leagueId.equals(edition.getLeague().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La edición no pertenece a la liga");
            }
            if (!encontrados.isEmpty()) {
                java.util.Set<UUID> inscritos = entries.findByEquiposEnEdicion(
                        encontrados.stream().map(Team::getId).collect(Collectors.toList()), editionId).stream()
                        .map(entry -> entry.getTeam().getId())
                        .collect(Collectors.toSet());
                encontrados = encontrados.stream()
                        .filter(team -> inscritos.contains(team.getId()))
                        .collect(Collectors.toList());
            }
        }
        Map<UUID, Long> jugadores = totalesJugadores(leagueId, editionId, encontrados);
        Map<UUID, List<CategoriaResumen>> categorias = categoriasPorEquipo(editionId, encontrados);
        return encontrados.stream()
                .map(team -> teamView(team, jugadores.getOrDefault(team.getId(), 0L), categorias.getOrDefault(team.getId(), List.of())))
                .collect(Collectors.toList());
    }

    public TeamView createTeam(TeamRequest request) {
        League league = league(request.getLeagueId());
        assertWrite(league.getId());
        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setLeague(league);
        applyTeam(team, request);
        auditarEquipo(team, true);
        teams.save(team);
        asignarCategoria(team, request.getCategoryId(), request.getCategoryIdActual());
        return teamView(team);
    }

    public TeamView updateTeam(UUID id, TeamRequest request) {
        Team team = team(id);
        League league = league(request.getLeagueId());
        assertWrite(team.getLeague().getId());
        assertWrite(league.getId());
        team.setLeague(league);
        applyTeam(team, request);
        auditarEquipo(team, false);
        asignarCategoria(team, request.getCategoryId(), request.getCategoryIdActual());
        return teamView(team);
    }

    public void deleteTeam(UUID id) {
        Team team = team(id);
        assertWrite(team.getLeague().getId());
        team.setEstado("INACTIVO");
        auditarEquipo(team, false);
    }

    @Transactional(readOnly = true)
    public Team teamLogo(UUID id) {
        Team team = team(id);
        assertRead(team.getLeague().getId());
        team.getLogo();
        return team;
    }

    @Transactional(readOnly = true)
    public List<TeamEntryView> listTeamEntries(UUID categoryId) {
        Category category = category(categoryId);
        assertRead(category.getEdition().getLeague().getId());
        return entries.findByCategory_IdOrderByTeam_NameAsc(categoryId).stream()
                .filter(entry -> "ACTIVO".equals(entry.getTeam().getEstado()))
                .map(this::teamEntryView)
                .collect(Collectors.toList());
    }

    public TeamEntryView createTeamEntry(TeamEntryRequest request) {
        Team team = team(request.getTeamId());
        Category category = category(request.getCategoryId());
        assertWrite(team.getLeague().getId());
        assertWrite(category.getEdition().getLeague().getId());
        if (!team.getLeague().getId().equals(category.getEdition().getLeague().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo y la categoría son de ligas distintas");
        }
        if (!"ACTIVO".equals(team.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede inscribir un equipo activo");
        }
        return entries.findByTeam_IdAndCategory_Id(team.getId(), category.getId())
                .map(this::teamEntryView)
                .orElseGet(() -> {
                    TeamEntry entry = new TeamEntry();
                    entry.setId(UUID.randomUUID());
                    entry.setTeam(team);
                    entry.setCategory(category);
                    return teamEntryView(entries.save(entry));
                });
    }

    @Transactional(readOnly = true)
    public List<PlayerView> listPlayers(UUID teamEntryId, String estado) {
        TeamEntry entry = teamEntry(teamEntryId);
        assertRead(entry.getTeam().getLeague().getId());
        String filtro = estado == null || estado.isBlank() ? "ACTIVO" : estado.trim().toUpperCase(Locale.ROOT);
        return players.findByTeamEntry_IdOrderByCedulaAsc(teamEntryId).stream()
                .filter(player -> "TODOS".equals(filtro) || filtro.equals(player.getEstado()))
                .map(this::playerView)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlayerView findByCedula(String cedula) {
        String value = cedula == null ? "" : cedula.trim();
        if (value.isEmpty()) {
            return null;
        }
        Player chosen = null;
        for (Player player : players.findByCedulaIgnoreCaseOrderByNameAsc(value)) {
            if (!canRead(player.getTeamEntry().getTeam().getLeague().getId())) {
                continue;
            }
            if (!"ACTIVO".equals(player.getTeamEntry().getTeam().getEstado())) {
                continue;
            }
            if (chosen == null || (chosen.getPhoto() == null && player.getPhoto() != null)) {
                chosen = player;
            }
        }
        return chosen == null ? null : playerView(chosen);
    }

    public PlayerView createPlayer(PlayerRequest request) {
        TeamEntry entry = teamEntry(request.getTeamEntryId());
        assertWrite(entry.getTeam().getLeague().getId());
        Player player = new Player();
        player.setId(UUID.randomUUID());
        player.setTeamEntry(entry);
        assertRosterRoom(entry, null);
        applyPlayer(player, request, true);
        return playerView(players.save(player));
    }

    public PlayerView updatePlayer(UUID id, PlayerRequest request) {
        Player player = player(id);
        TeamEntry entry = teamEntry(request.getTeamEntryId());
        assertWrite(player.getTeamEntry().getTeam().getLeague().getId());
        assertWrite(entry.getTeam().getLeague().getId());
        player.setTeamEntry(entry);
        assertRosterRoom(entry, player.getId());
        applyPlayer(player, request, false);
        return playerView(player);
    }

    public void deletePlayer(UUID id) {
        Player player = player(id);
        assertWrite(player.getTeamEntry().getTeam().getLeague().getId());
        players.delete(player);
    }

    @Transactional(readOnly = true)
    public Player playerPhoto(UUID id) {
        Player player = player(id);
        assertRead(player.getTeamEntry().getTeam().getLeague().getId());
        player.getPhoto();
        return player;
    }

    @Transactional(readOnly = true)
    public League leagueLogo(UUID id) {
        League league = league(id);
        assertRead(league.getId());
        league.getLogo();
        return league;
    }

    private void applyTeam(Team team, TeamRequest request) {
        team.setName(request.getName().trim());
        String representative = request.getNombreRepresentante() == null ? "" : request.getNombreRepresentante().trim();
        String phone = request.getNumeroTelefono() == null ? "" : request.getNumeroTelefono().trim();
        team.setNombreRepresentante(representative.isEmpty() ? null : representative);
        team.setNumeroTelefono(phone.isEmpty() ? null : phone);
        team.setEstado(estadoEquipo(request.getEstado()));
        if (request.isClearLogo()) {
            team.setLogo(null);
            team.setLogoContentType(null);
        }
        if (request.getLogoBase64() != null && !request.getLogoBase64().trim().isEmpty()) {
            byte[] logo = leerLogo(request.getLogoBase64(), request.getLogoContentType());
            team.setLogo(logo);
            team.setLogoContentType(imageType(request.getLogoContentType(), request.getLogoBase64()));
        }
    }

    private void asignarCategoria(Team team, UUID categoryId, UUID categoryIdActual) {
        if (categoryId == null) {
            return;
        }
        Category destino = category(categoryId);
        if (!"ACTIVO".equals(destino.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no está activa");
        }
        if (!team.getLeague().getId().equals(destino.getEdition().getLeague().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a la liga del equipo");
        }
        List<TeamEntry> enEdicion = entries.findByEquipoEnEdicion(team.getId(), destino.getEdition().getId());
        if (categoryIdActual != null) {
            TeamEntry actual = enEdicion.stream()
                    .filter(entry -> categoryIdActual.equals(entry.getCategory().getId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no está en la categoría actual"));
            if (destino.getId().equals(actual.getCategory().getId())) {
                return;
            }
            if (enEdicion.stream().anyMatch(entry -> destino.getId().equals(entry.getCategory().getId()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo ya está en esa categoría");
            }
            actual.setCategory(destino);
            return;
        }
        if (enEdicion.isEmpty()) {
            TeamEntry entry = new TeamEntry();
            entry.setId(UUID.randomUUID());
            entry.setTeam(team);
            entry.setCategory(destino);
            entries.save(entry);
            return;
        }
        if (enEdicion.size() == 1) {
            TeamEntry actual = enEdicion.get(0);
            if (!destino.getId().equals(actual.getCategory().getId())) {
                actual.setCategory(destino);
            }
            return;
        }
        if (enEdicion.stream().anyMatch(entry -> destino.getId().equals(entry.getCategory().getId()))) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo está en varias categorías. Elige la categoría actual");
    }

    private Map<UUID, List<CategoriaResumen>> categoriasPorEquipo(UUID editionId, List<Team> encontrados) {
        Map<UUID, List<CategoriaResumen>> categorias = new HashMap<>();
        if (editionId == null || encontrados.isEmpty()) {
            return categorias;
        }
        List<UUID> ids = encontrados.stream().map(Team::getId).collect(Collectors.toList());
        for (TeamEntry entry : entries.findByEquiposEnEdicion(ids, editionId)) {
            if (!"ACTIVO".equals(entry.getCategory().getEstado())) {
                continue;
            }
            categorias.computeIfAbsent(entry.getTeam().getId(), key -> new ArrayList<>())
                    .add(new CategoriaResumen(entry.getCategory().getId(), entry.getCategory().getName()));
        }
        categorias.values().forEach(lista -> lista.sort(Comparator.comparing(CategoriaResumen::getNombre, String.CASE_INSENSITIVE_ORDER)));
        return categorias;
    }

    private Map<UUID, Long> totalesJugadores(UUID leagueId, UUID editionId, List<Team> encontrados) {
        Map<UUID, Long> totales = new HashMap<>();
        if (editionId == null || encontrados.isEmpty()) {
            return totales;
        }
        Edition edition = editions.findById(editionId).orElse(null);
        if (edition == null || !leagueId.equals(edition.getLeague().getId())) {
            return totales;
        }
        List<UUID> ids = encontrados.stream().map(Team::getId).collect(Collectors.toList());
        for (Object[] fila : players.contarPorEquipo(editionId, ids)) {
            totales.put((UUID) fila[0], (Long) fila[1]);
        }
        return totales;
    }

    private byte[] leerLogo(String encoded, String declaredType) {
        String type = imageType(declaredType, encoded);
        if (!Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo debe ser JPEG, PNG, WEBP o GIF");
        }
        int comma = encoded.indexOf(',');
        String payload = encoded.startsWith("data:") && comma > 0 ? encoded.substring(comma + 1) : encoded.trim();
        byte[] logo;
        try {
            logo = Base64.getDecoder().decode(payload.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo no se pudo leer");
        }
        if (logo.length == 0 || logo.length > 2_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo debe pesar como máximo 2 MB");
        }
        return logo;
    }

    private void auditarEquipo(Team team, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        String usuario = currentUser().getUsername();
        if (nuevo) {
            team.setFechaCreacion(ahora);
            team.setUsuarioCreacion(usuario);
        }
        team.setFechaModificacion(ahora);
        team.setUsuarioModificacion(usuario);
    }

    private String estadoEquipo(String estado) {
        if (estado == null || estado.isBlank()) {
            return "ACTIVO";
        }
        String value = estado.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("ACTIVO", "BLOQUEADO", "INACTIVO").contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del equipo debe ser activo, bloqueado o inactivo");
        }
        return value;
    }

    private String estadoJugador(String estado) {
        if (estado == null || estado.isBlank()) {
            return "ACTIVO";
        }
        String value = estado.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("ACTIVO", "INACTIVO").contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del jugador debe ser activo o inactivo");
        }
        return value;
    }

    private String textoFiltro(String value) {
        return value == null ? "" : value.trim();
    }

    private void applyLeague(League league, LeagueRequest request) {
        league.setName(request.getName().trim());
        league.setValidFrom(request.getValidFrom());
        league.setValidUntil(request.getValidUntil());
        league.setActive(request.isActive());
        if (request.isClearLogo()) {
            league.getLogo();
            league.setLogo(null);
            league.setLogoContentType(null);
        }
        if (request.getLogoBase64() != null && !request.getLogoBase64().trim().isEmpty()) {
            applyLogo(league, request);
        }
    }

    private void applyLogo(League league, LeagueRequest request) {
        byte[] logo = leerLogo(request.getLogoBase64(), request.getLogoContentType());
        league.getLogo();
        league.setLogo(logo);
        league.setLogoContentType(imageType(request.getLogoContentType(), request.getLogoBase64()));
    }

    private void assertDateOrder(LocalDate start, LocalDate end, String message) {
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private League league(UUID id) {
        return leagues.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Liga no encontrada"));
    }

    private Edition edition(UUID id) {
        return editions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Edición no encontrada"));
    }

    private Category category(UUID id) {
        return categories.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private Team team(UUID id) {
        return teams.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado"));
    }

    private TeamEntry teamEntry(UUID id) {
        return entries.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscripción no encontrada"));
    }

    private Player player(UUID id) {
        return players.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado"));
    }

    private void assertRosterRoom(TeamEntry entry, UUID currentPlayerId) {
        long occupied = players.findByTeamEntry_IdOrderByCedulaAsc(entry.getId()).stream()
                .filter(existing -> "ACTIVO".equals(existing.getEstado()) && !existing.getId().equals(currentPlayerId))
                .count();
        if (occupied >= entry.getCategory().getMaxPlayers()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El plantel ya alcanzó el máximo de jugadores de la categoría.");
        }
    }

    private void applyPlayer(Player player, PlayerRequest request, boolean creating) {
        int rotation = request.getRotationDegrees();
        if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La rotación debe ser 0, 90, 180 o 270 grados");
        }
        player.setName(request.getName().trim());
        String alias = request.getAlias() == null ? "" : request.getAlias().trim();
        player.setAlias(alias.isEmpty() ? null : alias);
        player.setCedula(request.getCedula().trim());
        player.setNumeroCamisa(request.getNumeroCamisa());
        player.setRotationDegrees((short) rotation);
        player.setEstado(estadoJugador(request.getEstado()));
        if (request.isClearPhoto()) {
            player.getPhoto();
            player.setPhoto(null);
            player.setPhotoContentType(null);
        }
        if (request.getPhotoBase64() != null && !request.getPhotoBase64().trim().isEmpty()) {
            applyPhoto(player, request);
        } else if (creating) {
            player.setPhoto(null);
            player.setPhotoContentType(null);
        }
    }

    private void applyPhoto(Player player, PlayerRequest request) {
        String encoded = request.getPhotoBase64().trim();
        String type = imageType(request.getPhotoContentType(), encoded);
        if (!Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe ser JPEG, PNG, WEBP o GIF");
        }
        int comma = encoded.indexOf(',');
        if (encoded.startsWith("data:") && comma > 0) {
            encoded = encoded.substring(comma + 1);
        }
        byte[] photo;
        try {
            photo = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto no se pudo leer");
        }
        if (photo.length == 0 || photo.length > 1_500_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe pesar menos de 1.5 MB");
        }
        player.getPhoto();
        player.setPhoto(photo);
        player.setPhotoContentType(type);
    }

    private String imageType(String declared, String encoded) {
        String type = declared == null ? "" : declared.trim().toLowerCase();
        if (encoded.startsWith("data:")) {
            int semi = encoded.indexOf(';');
            if (semi > 5) {
                type = encoded.substring(5, semi).trim().toLowerCase();
            }
        }
        if ("image/jpg".equals(type) || "image/pjpeg".equals(type)) {
            return "image/jpeg";
        }
        return type;
    }

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findWithMembershipsByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void assertRead(UUID leagueId) {
        if (!canRead(leagueId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta liga");
        }
    }

    private boolean canRead(UUID leagueId) {
        AppUser user = currentUser();
        return user.isMaster() || memberOf(user, leagueId);
    }

    private void assertWrite(UUID leagueId) {
        AppUser user = currentUser();
        if (user.isMaster()) {
            return;
        }
        boolean admin = user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId)
                        && "ADMINISTRATIVO".equals(membership.getRole()));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta liga");
        }
    }

    private boolean memberOf(AppUser user, UUID leagueId) {
        return user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId));
    }

    private LeagueView leagueView(League league) {
        byte[] logo = league.getLogo();
        return new LeagueView(league.getId(), league.getName(), league.getValidFrom(), league.getValidUntil(), league.isActive(), logo != null && logo.length > 0);
    }

    private EditionView editionView(Edition edition) {
        return new EditionView(edition.getId(), edition.getLeague().getId(), edition.getLeague().getName(),
                edition.getName(), edition.getStartsOn(), edition.getEndsOn());
    }

    private List<Category> categoriasDe(UUID editionId, String estado) {
        String filtro = estado == null || estado.isBlank() ? "ACTIVO" : estado.trim().toUpperCase(Locale.ROOT);
        if ("TODOS".equals(filtro)) {
            return categories.findByEdition_IdOrderByNameAsc(editionId);
        }
        if (!"ACTIVO".equals(filtro) && !"INACTIVO".equals(filtro)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado de la categoría no es válido");
        }
        return categories.findByEdition_IdAndEstadoOrderByNameAsc(editionId, filtro);
    }

    private String estadoCategoria(String estado, String omision) {
        if (estado == null || estado.isBlank()) {
            return omision;
        }
        String filtro = estado.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVO".equals(filtro) && !"INACTIVO".equals(filtro)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado de la categoría no es válido");
        }
        return filtro;
    }

    private CategoryView categoryView(Category category) {
        Edition edition = category.getEdition();
        return new CategoryView(category.getId(), edition.getId(), edition.getName(),
                edition.getLeague().getId(), edition.getLeague().getName(),
                category.getName(), category.getMaxPlayers(), category.getCantidadGrupos(), category.getEquiposPorGrupo(),
                category.getCantidadFases(), category.getEstado());
    }

    private TeamView teamView(Team team) {
        return teamView(team, 0, List.of());
    }

    private TeamView teamView(Team team, long totalJugadores, List<CategoriaResumen> categorias) {
        byte[] logo = team.getLogo();
        return new TeamView(team.getId(), team.getLeague().getId(), team.getLeague().getName(), team.getName(),
                team.getNombreRepresentante(), team.getNumeroTelefono(), team.getEstado(),
                logo != null && logo.length > 0, totalJugadores, categorias);
    }

    private TeamEntryView teamEntryView(TeamEntry entry) {
        return new TeamEntryView(entry.getId(), entry.getTeam().getId(), entry.getTeam().getName(),
                entry.getCategory().getId(), entry.getCategory().getName(), entry.getTeam().getLeague().getId());
    }

    private PlayerView playerView(Player player) {
        TeamEntry entry = player.getTeamEntry();
        return new PlayerView(player.getId(), entry.getId(), entry.getTeam().getId(), entry.getTeam().getName(),
                entry.getCategory().getId(), player.getName(), player.getAlias(), player.getCedula(),
                player.getNumeroCamisa(),
                player.getRotationDegrees(), player.getEstado(), player.getPhoto() != null);
    }
}
