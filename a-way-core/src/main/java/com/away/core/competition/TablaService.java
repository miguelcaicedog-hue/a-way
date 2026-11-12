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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TablaService {

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final GrupoRepository grupos;
    private final GrupoEquipoRepository asignaciones;
    private final TeamEntryRepository entradas;
    private final AppUserRepository users;

    public TablaService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                        PartidoRepository partidos, GrupoRepository grupos, GrupoEquipoRepository asignaciones,
                        TeamEntryRepository entradas, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.grupos = grupos;
        this.asignaciones = asignaciones;
        this.entradas = entradas;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public TablaViews.TablaView listar(UUID editionId, UUID categoryId, LocalDate fecha) {
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

        Map<UUID, LocalDate> fechaJornada = new HashMap<>();
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoryId, "ACTIVO")) {
            fechaJornada.put(jornada.getId(), jornada.getFecha());
        }

        Map<UUID, UUID> grupoDeEquipo = new HashMap<>();
        Map<UUID, TablaGrupo> porGrupo = new LinkedHashMap<>();
        for (Grupo grupo : grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            porGrupo.put(grupo.getId(), new TablaGrupo(grupo.getId(), grupo.getNombre()));
        }
        for (GrupoEquipo asignacion : asignaciones.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            Team equipo = asignacion.getEquipo();
            if (equipo == null || "ELIMINADO".equals(equipo.getEstado())) {
                continue;
            }
            TablaGrupo tabla = porGrupo.get(asignacion.getIdGrupo());
            if (tabla == null) {
                continue;
            }
            grupoDeEquipo.put(equipo.getId(), tabla.id);
            tabla.filas.putIfAbsent(equipo.getId(), new Acumulado(equipo.getId(), equipo.getName()));
        }
        if (porGrupo.isEmpty()) {
            TablaGrupo unica = new TablaGrupo(null, "");
            porGrupo.put(null, unica);
            for (TeamEntry entrada : entradas.findByCategory_IdOrderByTeam_NameAsc(categoryId)) {
                Team equipo = entrada.getTeam();
                if (equipo == null || "ELIMINADO".equals(equipo.getEstado())) {
                    continue;
                }
                unica.filas.putIfAbsent(equipo.getId(), new Acumulado(equipo.getId(), equipo.getName()));
            }
        }

        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            if (partido.getIdCruce() != null || partido.getIdEquipoLocal() == null || partido.getIdEquipoVisita() == null) {
                continue;
            }
            if (partido.getGolesLocal() == null || partido.getGolesVisita() == null) {
                continue;
            }
            LocalDate dia = partido.getFecha() != null ? partido.getFecha() : fechaJornada.get(partido.getIdJornada());
            if (dia == null || dia.isAfter(fecha)) {
                continue;
            }
            TablaGrupo tabla = tablaDelPartido(partido, grupoDeEquipo, porGrupo);
            if (tabla == null) {
                continue;
            }
            sumar(tabla, partido.getIdEquipoLocal(), partido.getGolesLocal(), partido.getGolesVisita());
            sumar(tabla, partido.getIdEquipoVisita(), partido.getGolesVisita(), partido.getGolesLocal());
        }

        List<TablaViews.GrupoView> resultado = new ArrayList<>();
        for (TablaGrupo tabla : porGrupo.values()) {
            if (tabla.filas.isEmpty()) {
                continue;
            }
            List<Acumulado> ordenadas = new ArrayList<>(tabla.filas.values());
            ordenadas.sort(Comparator.comparingInt(Acumulado::getPuntos).reversed()
                    .thenComparing(Comparator.comparingInt(Acumulado::getDiferencia).reversed())
                    .thenComparing(Comparator.comparingInt(Acumulado::getGolesFavor).reversed())
                    .thenComparing(fila -> fila.equipo.toLowerCase()));
            List<TablaViews.FilaView> filas = new ArrayList<>();
            int posicion = 1;
            for (Acumulado fila : ordenadas) {
                filas.add(new TablaViews.FilaView(
                        posicion++,
                        fila.teamId,
                        fila.equipo,
                        fila.pj,
                        fila.ganados,
                        fila.empatados,
                        fila.perdidos,
                        fila.puntos(),
                        fila.golesFavor,
                        fila.golesContra,
                        fila.diferencia()));
            }
            resultado.add(new TablaViews.GrupoView(tabla.id, tabla.nombre, filas));
        }
        return new TablaViews.TablaView(fecha, resultado);
    }

    private TablaGrupo tablaDelPartido(Partido partido, Map<UUID, UUID> grupoDeEquipo, Map<UUID, TablaGrupo> porGrupo) {
        if (porGrupo.containsKey(null)) {
            return porGrupo.get(null);
        }
        UUID grupoId = partido.getIdGrupo();
        if (grupoId == null) {
            UUID local = grupoDeEquipo.get(partido.getIdEquipoLocal());
            UUID visita = grupoDeEquipo.get(partido.getIdEquipoVisita());
            if (local != null && local.equals(visita)) {
                grupoId = local;
            }
        }
        return grupoId == null ? null : porGrupo.get(grupoId);
    }

    private void sumar(TablaGrupo tabla, UUID teamId, int favor, int contra) {
        Acumulado fila = tabla.filas.get(teamId);
        if (fila == null) {
            return;
        }
        fila.pj++;
        fila.golesFavor += favor;
        fila.golesContra += contra;
        if (favor > contra) {
            fila.ganados++;
        } else if (favor == contra) {
            fila.empatados++;
        } else {
            fila.perdidos++;
        }
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

    private static final class TablaGrupo {
        private final UUID id;
        private final String nombre;
        private final Map<UUID, Acumulado> filas = new LinkedHashMap<>();

        private TablaGrupo(UUID id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    private static final class Acumulado {
        private final UUID teamId;
        private final String equipo;
        private int pj;
        private int ganados;
        private int empatados;
        private int perdidos;
        private int golesFavor;
        private int golesContra;

        private Acumulado(UUID teamId, String equipo) {
            this.teamId = teamId;
            this.equipo = equipo;
        }

        private int puntos() {
            return ganados * 3 + empatados;
        }

        private int diferencia() {
            return golesFavor - golesContra;
        }

        private int getPuntos() {
            return puntos();
        }

        private int getDiferencia() {
            return diferencia();
        }

        private int getGolesFavor() {
            return golesFavor;
        }
    }
}
