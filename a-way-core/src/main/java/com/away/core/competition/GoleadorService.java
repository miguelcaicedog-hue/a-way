package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GoleadorService {

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final GolJugadorRepository goles;
    private final PlayerRepository jugadores;
    private final TeamRepository teams;
    private final TeamEntryRepository entradas;
    private final AppUserRepository users;

    public GoleadorService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                           PartidoRepository partidos, GolJugadorRepository goles, PlayerRepository jugadores,
                           TeamRepository teams, TeamEntryRepository entradas, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.goles = goles;
        this.jugadores = jugadores;
        this.teams = teams;
        this.entradas = entradas;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public GoleadorViews.PanelView listar(UUID editionId, UUID categoryId, LocalDate fecha, UUID teamId) {
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una categoría");
        }
        if (fecha == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una fecha");
        }
        Edition edition = editions.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
        assertRead(edition.getLeague().getId());
        Category categoria = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        if (!categoria.getEdition().getId().equals(edition.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a esta edición");
        }

        Map<UUID, String> nombresEquipo = new HashMap<>();
        List<GoleadorViews.EquipoView> equipos = new ArrayList<>();
        Set<UUID> equiposCategoria = new HashSet<>();
        for (TeamEntry entrada : entradas.findByCategory_IdOrderByTeam_NameAsc(categoryId)) {
            Team equipo = entrada.getTeam();
            if (equipo == null || "ELIMINADO".equals(equipo.getEstado())) {
                continue;
            }
            if (equiposCategoria.add(equipo.getId())) {
                nombresEquipo.put(equipo.getId(), equipo.getName());
                equipos.add(new GoleadorViews.EquipoView(equipo.getId(), equipo.getName()));
            }
        }
        equipos.sort(Comparator.comparing(equipo -> equipo.getNombre().toLowerCase()));
        if (teamId != null && !equiposCategoria.contains(teamId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no pertenece a esta categoría");
        }

        Map<UUID, Jornada> porJornada = new HashMap<>();
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoryId, "ACTIVO")) {
            porJornada.put(jornada.getId(), jornada);
        }
        Map<UUID, Partido> porPartido = new HashMap<>();
        Set<UUID> equiposPartido = new HashSet<>();
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            LocalDate dia = partido.getFecha() != null ? partido.getFecha() : fechaDe(porJornada.get(partido.getIdJornada()));
            if (dia == null || dia.isAfter(fecha)) {
                continue;
            }
            porPartido.put(partido.getId(), partido);
            if (partido.getIdEquipoLocal() != null) {
                equiposPartido.add(partido.getIdEquipoLocal());
            }
            if (partido.getIdEquipoVisita() != null) {
                equiposPartido.add(partido.getIdEquipoVisita());
            }
        }
        equiposPartido.removeAll(nombresEquipo.keySet());
        if (!equiposPartido.isEmpty()) {
            for (Team equipo : teams.findAllById(equiposPartido)) {
                nombresEquipo.put(equipo.getId(), equipo.getName());
            }
        }

        Map<UUID, Acumulado> porJugador = new HashMap<>();
        for (GolJugador gol : goles.activosDeCategoria(categoryId)) {
            Partido partido = porPartido.get(gol.getIdPartido());
            if (partido == null || gol.getCantidad() <= 0) {
                continue;
            }
            Acumulado fila = porJugador.computeIfAbsent(gol.getIdJugador(), Acumulado::new);
            fila.sumar(partido, gol.getCantidad(), porJornada);
        }

        List<GoleadorViews.GoleadorView> goleadores = new ArrayList<>();
        if (!porJugador.isEmpty()) {
            Map<UUID, Player> porId = new HashMap<>();
            for (Player jugador : jugadores.findAllById(porJugador.keySet())) {
                porId.put(jugador.getId(), jugador);
            }
            for (Acumulado fila : porJugador.values()) {
                Player jugador = porId.get(fila.jugadorId);
                if (jugador == null || jugador.getTeamEntry() == null || jugador.getTeamEntry().getTeam() == null) {
                    continue;
                }
                if (!categoryId.equals(jugador.getTeamEntry().getCategory().getId())) {
                    continue;
                }
                Team equipo = jugador.getTeamEntry().getTeam();
                if (teamId != null && !teamId.equals(equipo.getId())) {
                    continue;
                }
                fila.detalle.sort(Comparator.comparing((DetalleItem item) -> item.fecha, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(item -> item.numero)
                        .thenComparingInt(item -> item.orden));
                List<GoleadorViews.DetalleView> detalle = new ArrayList<>();
                for (DetalleItem item : fila.detalle) {
                    detalle.add(new GoleadorViews.DetalleView(
                            item.jornada, item.fecha, item.rivalDe(equipo.getId(), nombresEquipo), item.cantidad));
                }
                String alias = jugador.getAlias() == null ? "" : jugador.getAlias().trim();
                goleadores.add(new GoleadorViews.GoleadorView(
                        jugador.getId(),
                        alias,
                        jugador.getName(),
                        equipo.getId(),
                        equipo.getName(),
                        fila.goles,
                        jugador.getPhoto() != null && jugador.getPhoto().length > 0,
                        jugador.getRotationDegrees(),
                        detalle));
            }
        }
        goleadores.sort(Comparator.comparingInt(GoleadorViews.GoleadorView::getGoles).reversed()
                .thenComparing(fila -> etiqueta(fila).toLowerCase()));
        return new GoleadorViews.PanelView(fecha, equipos, goleadores);
    }

    private LocalDate fechaDe(Jornada jornada) {
        return jornada == null ? null : jornada.getFecha();
    }

    private String etiqueta(GoleadorViews.GoleadorView fila) {
        return fila.getAlias().isEmpty() ? fila.getNombre() : fila.getAlias();
    }

    private void assertRead(UUID leagueId) {
        AppUser user = users.findWithMembershipsByUsername(
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        boolean miembro = user.isMaster() || user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId));
        if (!miembro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta liga");
        }
    }

    private static final class Acumulado {
        private final UUID jugadorId;
        private int goles;
        private final List<DetalleItem> detalle = new ArrayList<>();

        private Acumulado(UUID jugadorId) {
            this.jugadorId = jugadorId;
        }

        private void sumar(Partido partido, int cantidad, Map<UUID, Jornada> porJornada) {
            goles += cantidad;
            for (DetalleItem item : detalle) {
                if (partido.getId().equals(item.partidoId)) {
                    item.cantidad += cantidad;
                    return;
                }
            }
            Jornada jornada = porJornada.get(partido.getIdJornada());
            LocalDate dia = partido.getFecha() != null ? partido.getFecha() : jornada == null ? null : jornada.getFecha();
            detalle.add(new DetalleItem(
                    partido.getId(),
                    jornada == null ? "Sin jornada" : jornada.getNombre(),
                    dia,
                    jornada == null ? 0 : jornada.getNumero(),
                    partido.getOrden(),
                    partido.getIdEquipoLocal(),
                    partido.getIdEquipoVisita(),
                    cantidad));
        }
    }

    private static final class DetalleItem {
        private final UUID partidoId;
        private final String jornada;
        private final LocalDate fecha;
        private final int numero;
        private final int orden;
        private final UUID equipoLocal;
        private final UUID equipoVisita;
        private int cantidad;

        private DetalleItem(UUID partidoId, String jornada, LocalDate fecha, int numero, int orden,
                            UUID equipoLocal, UUID equipoVisita, int cantidad) {
            this.partidoId = partidoId;
            this.jornada = jornada;
            this.fecha = fecha;
            this.numero = numero;
            this.orden = orden;
            this.equipoLocal = equipoLocal;
            this.equipoVisita = equipoVisita;
            this.cantidad = cantidad;
        }

        private String rivalDe(UUID equipoJugador, Map<UUID, String> nombresEquipo) {
            UUID rival = equipoJugador != null && equipoJugador.equals(equipoLocal) ? equipoVisita : equipoLocal;
            if (rival == null) {
                return "Por definir";
            }
            return nombresEquipo.getOrDefault(rival, "Por definir");
        }
    }
}
