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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final CriterioDesempateEquipoService criterios;
    private final TarjetaJugadorRepository tarjetasJugador;
    private final TarjetaRepository tarjetas;
    private final PlayerRepository jugadores;
    private final AppUserRepository users;

    public TablaService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                        PartidoRepository partidos, GrupoRepository grupos, GrupoEquipoRepository asignaciones,
                        TeamEntryRepository entradas, CriterioDesempateEquipoService criterios,
                        TarjetaJugadorRepository tarjetasJugador, TarjetaRepository tarjetas,
                        PlayerRepository jugadores, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.grupos = grupos;
        this.asignaciones = asignaciones;
        this.entradas = entradas;
        this.criterios = criterios;
        this.tarjetasJugador = tarjetasJugador;
        this.tarjetas = tarjetas;
        this.jugadores = jugadores;
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
            tabla.encuentros.add(new Encuentro(partido.getId(), partido.getIdEquipoLocal(), partido.getIdEquipoVisita(),
                    partido.getGolesLocal(), partido.getGolesVisita()));
        }

        List<String> ordenCriterios = criterios.orden(edition.getId());
        int pesoAmarilla = peso(cercano(edition.getLeague().getId(), true, null));
        int pesoRoja = peso(cercano(edition.getLeague().getId(), false, cercano(edition.getLeague().getId(), true, null)));
        Map<UUID, UUID> equipoDeJugador = equipoDeJugador(categoryId);
        List<TarjetaJugador> tarjetasCategoria = tarjetasJugador.activasDeCategoria(categoryId);
        List<TablaViews.GrupoView> resultado = new ArrayList<>();
        for (TablaGrupo tabla : porGrupo.values()) {
            if (tabla.filas.isEmpty()) {
                continue;
            }
            Map<UUID, Integer> fairPlay = fairPlay(tabla.encuentros, tarjetasCategoria, equipoDeJugador, pesoAmarilla, pesoRoja);
            List<Acumulado> ordenadas = ordenar(new ArrayList<>(tabla.filas.values()), ordenCriterios, fairPlay, tabla.encuentros);
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

    @Transactional(readOnly = true)
    public Map<UUID, Integer> puestos(UUID categoryId, LocalDate fecha) {
        Category categoria = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        Map<UUID, Integer> puestos = new HashMap<>();
        for (TablaViews.GrupoView grupo : listar(categoria.getEdition().getId(), categoryId, fecha).getGrupos()) {
            for (TablaViews.FilaView fila : grupo.getEquipos()) {
                puestos.put(fila.getTeamId(), fila.getPosicion());
            }
        }
        return puestos;
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

    private List<Acumulado> ordenar(List<Acumulado> filas, List<String> criteriosOrden, Map<UUID, Integer> fairPlay,
                                   List<Encuentro> encuentros) {
        filas.sort(Comparator.comparingInt(Acumulado::getPuntos).reversed()
                .thenComparing(Comparator.comparingInt(Acumulado::getDiferencia).reversed())
                .thenComparing(fila -> fila.equipo.toLowerCase()));
        List<Acumulado> ordenadas = new ArrayList<>();
        int inicio = 0;
        while (inicio < filas.size()) {
            Acumulado actual = filas.get(inicio);
            int fin = inicio + 1;
            while (fin < filas.size()
                    && filas.get(fin).puntos() == actual.puntos()
                    && filas.get(fin).diferencia() == actual.diferencia()) {
                fin++;
            }
            List<Acumulado> grupo = new ArrayList<>(filas.subList(inicio, fin));
            if (grupo.size() > 1) {
                Map<UUID, Integer> directo = directo(grupo, encuentros);
                grupo.sort((izquierda, derecha) -> comparar(izquierda, derecha, criteriosOrden, fairPlay, directo));
            }
            ordenadas.addAll(grupo);
            inicio = fin;
        }
        return ordenadas;
    }

    private int comparar(Acumulado izquierda, Acumulado derecha, List<String> criteriosOrden,
                         Map<UUID, Integer> fairPlay, Map<UUID, Integer> directo) {
        for (String codigo : criteriosOrden) {
            int diferencia = 0;
            if ("FAIR_PLAY".equals(codigo)) {
                diferencia = Integer.compare(fairPlay.getOrDefault(izquierda.teamId, 0), fairPlay.getOrDefault(derecha.teamId, 0));
            } else if ("GOLES_FAVOR".equals(codigo)) {
                diferencia = Integer.compare(derecha.golesFavor, izquierda.golesFavor);
            } else if ("ENFRENTAMIENTO_DIRECTO".equals(codigo)) {
                diferencia = Integer.compare(directo.getOrDefault(derecha.teamId, 0), directo.getOrDefault(izquierda.teamId, 0));
            }
            if (diferencia != 0) {
                return diferencia;
            }
        }
        return izquierda.equipo.compareToIgnoreCase(derecha.equipo);
    }

    private Map<UUID, Integer> directo(List<Acumulado> grupo, List<Encuentro> encuentros) {
        Set<UUID> equipos = new HashSet<>();
        for (Acumulado fila : grupo) {
            equipos.add(fila.teamId);
        }
        Map<UUID, Integer> puntos = new HashMap<>();
        for (Encuentro encuentro : encuentros) {
            if (!equipos.contains(encuentro.local) || !equipos.contains(encuentro.visita)) {
                continue;
            }
            if (encuentro.golesLocal > encuentro.golesVisita) {
                puntos.merge(encuentro.local, 3, Integer::sum);
            } else if (encuentro.golesLocal < encuentro.golesVisita) {
                puntos.merge(encuentro.visita, 3, Integer::sum);
            } else {
                puntos.merge(encuentro.local, 1, Integer::sum);
                puntos.merge(encuentro.visita, 1, Integer::sum);
            }
        }
        return puntos;
    }

    private Map<UUID, UUID> equipoDeJugador(UUID categoryId) {
        Map<UUID, UUID> equipoDeJugador = new HashMap<>();
        for (TeamEntry entrada : entradas.findByCategory_IdOrderByTeam_NameAsc(categoryId)) {
            if (entrada.getTeam() == null) {
                continue;
            }
            for (Player jugador : jugadores.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId())) {
                equipoDeJugador.put(jugador.getId(), entrada.getTeam().getId());
            }
        }
        return equipoDeJugador;
    }

    private Map<UUID, Integer> fairPlay(List<Encuentro> encuentros, List<TarjetaJugador> tarjetas, Map<UUID, UUID> equipoDeJugador,
                                        int pesoAmarilla, int pesoRoja) {
        Set<UUID> partidos = new HashSet<>();
        for (Encuentro encuentro : encuentros) {
            partidos.add(encuentro.id);
        }
        Map<UUID, Integer> totales = new HashMap<>();
        for (TarjetaJugador tarjeta : tarjetas) {
            if (!partidos.contains(tarjeta.getIdPartido())) {
                continue;
            }
            UUID equipo = equipoDeJugador.get(tarjeta.getIdJugador());
            if (equipo == null) {
                continue;
            }
            int amarillas = tarjeta.getAmarillas() == null ? 0 : tarjeta.getAmarillas();
            int rojas = tarjeta.getRojas() == null ? 0 : tarjeta.getRojas();
            totales.merge(equipo, amarillas * pesoAmarilla + rojas * pesoRoja, Integer::sum);
        }
        return totales;
    }

    private int peso(Tarjeta tarjeta) {
        return tarjeta == null || tarjeta.getPesoFairPlay() < 0 ? 1 : tarjeta.getPesoFairPlay();
    }

    private Tarjeta cercano(UUID ligaId, boolean amarillo, Tarjeta excepto) {
        Tarjeta mejor = null;
        int distancia = Integer.MAX_VALUE;
        int objetivoR = amarillo ? 0xF5 : 0xC6;
        int objetivoG = amarillo ? 0xC5 : 0x28;
        int objetivoB = amarillo ? 0x18 : 0x28;
        UUID excluido = excepto == null ? null : excepto.getId();
        for (Tarjeta tarjeta : tarjetas.buscar(ligaId, "", "ACTIVO")) {
            if (excluido != null && excluido.equals(tarjeta.getId())) {
                continue;
            }
            int actual = distancia(tarjeta.getColor(), objetivoR, objetivoG, objetivoB);
            if (actual < distancia) {
                distancia = actual;
                mejor = tarjeta;
            }
        }
        return mejor;
    }

    private int distancia(String color, int rojo, int verde, int azul) {
        if (color == null || !color.matches("#[0-9A-Fa-f]{6}")) {
            return Integer.MAX_VALUE;
        }
        int valor = Integer.parseInt(color.substring(1), 16);
        int dr = ((valor >> 16) & 255) - rojo;
        int dg = ((valor >> 8) & 255) - verde;
        int db = (valor & 255) - azul;
        return dr * dr + dg * dg + db * db;
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
        private final List<Encuentro> encuentros = new ArrayList<>();

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

    private static final class Encuentro {
        private final UUID id;
        private final UUID local;
        private final UUID visita;
        private final int golesLocal;
        private final int golesVisita;

        private Encuentro(UUID id, UUID local, UUID visita, int golesLocal, int golesVisita) {
            this.id = id;
            this.local = local;
            this.visita = visita;
            this.golesLocal = golesLocal;
            this.golesVisita = golesVisita;
        }
    }
}
