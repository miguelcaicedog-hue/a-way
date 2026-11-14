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
    private final CriterioDesempateGoleadorService criterios;
    private final TarjetaJugadorRepository tarjetasJugador;
    private final TarjetaRepository tarjetas;
    private final TablaService tablas;
    private final AppUserRepository users;

    public GoleadorService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                           PartidoRepository partidos, GolJugadorRepository goles, PlayerRepository jugadores,
                           TeamRepository teams, TeamEntryRepository entradas, CriterioDesempateGoleadorService criterios,
                           TarjetaJugadorRepository tarjetasJugador, TarjetaRepository tarjetas, TablaService tablas,
                           AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.goles = goles;
        this.jugadores = jugadores;
        this.teams = teams;
        this.entradas = entradas;
        this.criterios = criterios;
        this.tarjetasJugador = tarjetasJugador;
        this.tarjetas = tarjetas;
        this.tablas = tablas;
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
        Map<UUID, Map<UUID, Integer>> golesContra = new HashMap<>();
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
                golesContra.put(jugador.getId(), golesContra(fila, equipo.getId()));
            }
        }
        ordenar(goleadores, criterios.orden(edition.getId()), fairPlay(edition.getLeague().getId(), porPartido),
                tablas.puestos(categoryId, fecha), golesContra);
        return new GoleadorViews.PanelView(fecha, equipos, goleadores);
    }

    private void ordenar(List<GoleadorViews.GoleadorView> goleadores, List<String> criteriosOrden,
                         Map<UUID, Integer> fairPlay, Map<UUID, Integer> puestos,
                         Map<UUID, Map<UUID, Integer>> golesContra) {
        goleadores.sort(Comparator.comparingInt(GoleadorViews.GoleadorView::getGoles).reversed()
                .thenComparing(fila -> etiqueta(fila).toLowerCase()));
        List<GoleadorViews.GoleadorView> ordenados = new ArrayList<>();
        int inicio = 0;
        while (inicio < goleadores.size()) {
            int goles = goleadores.get(inicio).getGoles();
            int fin = inicio + 1;
            while (fin < goleadores.size() && goleadores.get(fin).getGoles() == goles) {
                fin++;
            }
            List<GoleadorViews.GoleadorView> grupo = new ArrayList<>(goleadores.subList(inicio, fin));
            if (grupo.size() > 1) {
                Map<UUID, Integer> directo = directo(grupo, golesContra);
                grupo.sort((izquierda, derecha) -> comparar(izquierda, derecha, criteriosOrden, fairPlay, directo, puestos));
            }
            ordenados.addAll(grupo);
            inicio = fin;
        }
        goleadores.clear();
        goleadores.addAll(ordenados);
    }

    private int comparar(GoleadorViews.GoleadorView izquierda, GoleadorViews.GoleadorView derecha, List<String> criteriosOrden,
                         Map<UUID, Integer> fairPlay, Map<UUID, Integer> directo, Map<UUID, Integer> puestos) {
        for (String codigo : criteriosOrden) {
            int diferencia = 0;
            if ("FAIR_PLAY".equals(codigo)) {
                diferencia = Integer.compare(fairPlay.getOrDefault(izquierda.getJugadorId(), 0),
                        fairPlay.getOrDefault(derecha.getJugadorId(), 0));
            } else if ("ENFRENTAMIENTO_DIRECTO".equals(codigo)) {
                diferencia = Integer.compare(directo.getOrDefault(derecha.getJugadorId(), 0),
                        directo.getOrDefault(izquierda.getJugadorId(), 0));
            } else if ("POSICION_TABLA".equals(codigo)) {
                diferencia = Integer.compare(puestos.getOrDefault(izquierda.getEquipoId(), Integer.MAX_VALUE),
                        puestos.getOrDefault(derecha.getEquipoId(), Integer.MAX_VALUE));
            }
            if (diferencia != 0) {
                return diferencia;
            }
        }
        return etiqueta(izquierda).compareToIgnoreCase(etiqueta(derecha));
    }

    private Map<UUID, Integer> directo(List<GoleadorViews.GoleadorView> grupo, Map<UUID, Map<UUID, Integer>> golesContra) {
        Set<UUID> equipos = new HashSet<>();
        for (GoleadorViews.GoleadorView fila : grupo) {
            equipos.add(fila.getEquipoId());
        }
        Map<UUID, Integer> totales = new HashMap<>();
        for (GoleadorViews.GoleadorView fila : grupo) {
            int goles = 0;
            Map<UUID, Integer> contra = golesContra.getOrDefault(fila.getJugadorId(), Map.of());
            for (Map.Entry<UUID, Integer> rival : contra.entrySet()) {
                if (equipos.contains(rival.getKey()) && !rival.getKey().equals(fila.getEquipoId())) {
                    goles += rival.getValue();
                }
            }
            totales.put(fila.getJugadorId(), goles);
        }
        return totales;
    }

    private Map<UUID, Integer> golesContra(Acumulado fila, UUID equipoId) {
        Map<UUID, Integer> contra = new HashMap<>();
        for (DetalleItem item : fila.detalle) {
            UUID rival = equipoId != null && equipoId.equals(item.equipoLocal) ? item.equipoVisita : item.equipoLocal;
            if (rival != null) {
                contra.merge(rival, item.cantidad, Integer::sum);
            }
        }
        return contra;
    }

    private Map<UUID, Integer> fairPlay(UUID leagueId, Map<UUID, Partido> porPartido) {
        int pesoAmarilla = peso(cercano(leagueId, true, null));
        int pesoRoja = peso(cercano(leagueId, false, cercano(leagueId, true, null)));
        Map<UUID, Integer> totales = new HashMap<>();
        if (porPartido.isEmpty()) {
            return totales;
        }
        for (TarjetaJugador tarjeta : tarjetasJugador.activasDeCategoria(porPartido.values().iterator().next().getIdCategoria())) {
            if (!porPartido.containsKey(tarjeta.getIdPartido())) {
                continue;
            }
            int amarillas = tarjeta.getAmarillas() == null ? 0 : tarjeta.getAmarillas();
            int rojas = tarjeta.getRojas() == null ? 0 : tarjeta.getRojas();
            totales.merge(tarjeta.getIdJugador(), amarillas * pesoAmarilla + rojas * pesoRoja, Integer::sum);
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
