package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SuspensionesJugadorService {

    private static final String JORNADA_SIGUIENTE = "NEXT_MATCHDAY";
    private static final String PRIMERA_FECHA = "FIRST_MATCHDAY_OF_DAY";

    private final CategoryRepository categories;
    private final TeamEntryRepository entradas;
    private final PlayerRepository jugadores;
    private final JornadaRepository jornadas;
    private final SuspensionJugadorRepository suspensiones;
    private final SuspensionExclusionRepository exclusiones;
    private final ReglaSuspensionRepository reglas;
    private final LeagueSettingsRepository ajustes;
    private final AppUserRepository users;

    public SuspensionesJugadorService(CategoryRepository categories,
                                      TeamEntryRepository entradas, PlayerRepository jugadores,
                                      JornadaRepository jornadas, SuspensionJugadorRepository suspensiones,
                                      SuspensionExclusionRepository exclusiones, ReglaSuspensionRepository reglas,
                                      LeagueSettingsRepository ajustes, AppUserRepository users) {
        this.categories = categories;
        this.entradas = entradas;
        this.jugadores = jugadores;
        this.jornadas = jornadas;
        this.suspensiones = suspensiones;
        this.exclusiones = exclusiones;
        this.reglas = reglas;
        this.ajustes = ajustes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public SuspensionesJugadorViews.PanelView listar(UUID editionId, UUID categoryId, UUID teamId, String alias, Integer numero) {
        Category categoria = categoria(editionId, categoryId);
        assertStaff(categoria.getEdition().getLeague().getId());
        TeamEntry entrada = entrada(categoryId, teamId);
        List<Jornada> orden = ordenadas(categoryId);
        int proxima = proxima(orden, LocalDate.now());
        Map<UUID, List<SuspensionJugador>> porJugador = porJugador(categoryId);
        String texto = alias == null ? "" : alias.trim().toLowerCase();
        List<SuspensionesJugadorViews.JugadorView> filas = new ArrayList<>();
        for (Player jugador : jugadores.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId())) {
            if (!"ACTIVO".equals(jugador.getEstado())) {
                continue;
            }
            String nombre = alias(jugador);
            if (!texto.isEmpty() && !nombre.toLowerCase().contains(texto)) {
                continue;
            }
            if (numero != null && jugador.getNumeroCamisa() != numero) {
                continue;
            }
            int pendientes = pendientes(porJugador.getOrDefault(jugador.getId(), List.of()), orden, proxima);
            filas.add(new SuspensionesJugadorViews.JugadorView(
                    jugador.getId(), nombre, jugador.getNumeroCamisa(), entrada.getTeam().getName(),
                    jugador.getPhoto() != null, jugador.getRotationDegrees(), pendientes));
        }
        filas.sort(Comparator.comparingInt(SuspensionesJugadorViews.JugadorView::getNumero)
                .thenComparing(SuspensionesJugadorViews.JugadorView::getAlias));
        return new SuspensionesJugadorViews.PanelView(vista(orden, proxima), filas);
    }

    @Transactional(readOnly = true)
    public SuspensionesJugadorViews.DetalleView detalle(UUID jugadorId, UUID categoryId) {
        Player jugador = jugador(jugadorId);
        Category categoria = categoriaDe(jugador, categoryId);
        assertStaff(categoria.getEdition().getLeague().getId());
        List<Jornada> orden = ordenadas(categoryId);
        Map<UUID, Jornada> porId = new HashMap<>();
        List<SuspensionesJugadorViews.JornadaView> calendario = new ArrayList<>();
        for (Jornada jornada : orden) {
            porId.put(jornada.getId(), jornada);
            calendario.add(new SuspensionesJugadorViews.JornadaView(jornada.getId(), jornada.getNumero(), jornada.getNombre(), jornada.getFecha()));
        }
        List<SuspensionesJugadorViews.SuspensionView> activas = new ArrayList<>();
        for (SuspensionJugador suspension : suspensiones.findByIdCategoriaAndIdJugadorAndEstado(categoryId, jugadorId, "ACTIVO")) {
            Jornada inicio = porId.get(suspension.getIdJornadaInicio());
            activas.add(new SuspensionesJugadorViews.SuspensionView(
                    suspension.getId(), suspension.getMotivo(), etiqueta(suspension.getMotivo()),
                    "FALTA_GRAVE".equals(suspension.getMotivo()) ? "Todas las jornadas" : texto(inicio),
                    suspension.getJornadas(), suspension.getOrigen() == null ? "CIERRE" : suspension.getOrigen()));
        }
        return new SuspensionesJugadorViews.DetalleView(
                jugador.getId(), alias(jugador), jugador.getTeamEntry().getTeam().getName(),
                opciones(categoria), calendario, activas);
    }

    @Transactional
    public SuspensionesJugadorViews.DetalleView agregar(SuspensionesJugadorViews.AgregarRequest request) {
        if (request == null || request.getJugadorId() == null || request.getCategoryId() == null || request.getMotivo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige el jugador y la suspensión");
        }
        Player jugador = jugador(request.getJugadorId());
        Category categoria = categoriaDe(jugador, request.getCategoryId());
        assertStaff(categoria.getEdition().getLeague().getId());
        String motivo = request.getMotivo();
        List<Jornada> orden = ordenadas(categoria.getId());
        if (orden.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no tiene jornadas");
        }
        List<SuspensionJugador> activas = suspensiones.findByIdCategoriaAndIdJugadorAndEstado(categoria.getId(), jugador.getId(), "ACTIVO");
        Regla regla = regla(categoria);
        if ("FALTA_GRAVE".equals(motivo)) {
            if (!regla.faltaGrave) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa suspensión no está activa");
            }
            for (SuspensionJugador suspension : activas) {
                if ("FALTA_GRAVE".equals(suspension.getMotivo())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador ya está eliminado del campeonato");
                }
            }
            guardar(categoria, jugador.getId(), orden.get(0).getId(), orden.get(0).getId(), orden.size(), motivo);
            return detalle(jugador.getId(), categoria.getId());
        }
        if (!activa(regla, motivo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa suspensión no está activa");
        }
        int cantidad = "SANCION".equals(motivo) ? jornadasPedidas(request.getJornadas()) : cantidad(regla, motivo);
        if (cantidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa suspensión no tiene jornadas configuradas");
        }
        int preferido = "SANCION".equals(motivo)
                ? indiceDe(orden, request.getJornadaInicioId())
                : inicio(orden, momento(regla, motivo), LocalDate.now());
        if (preferido < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay una jornada donde iniciar la suspensión");
        }
        if ("SANCION".equals(motivo)) {
            if (preferido + cantidad > orden.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay jornadas suficientes desde la jornada elegida");
            }
            Set<Integer> tomadas = ocupadas(activas, orden);
            for (int paso = 0; paso < cantidad; paso++) {
                if (tomadas.contains(preferido + paso)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sanción pisa otra suspensión del jugador");
                }
            }
            guardar(categoria, jugador.getId(), orden.get(preferido).getId(), orden.get(preferido).getId(), cantidad, motivo);
            return detalle(jugador.getId(), categoria.getId());
        }
        int cursor = libre(orden, ocupadas(activas, orden), preferido, cantidad);
        if (cursor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay jornadas libres para esta suspensión");
        }
        guardar(categoria, jugador.getId(), orden.get(cursor).getId(), orden.get(cursor).getId(), cantidad, motivo);
        return detalle(jugador.getId(), categoria.getId());
    }

    @Transactional
    public void quitar(UUID suspensionId) {
        SuspensionJugador suspension = suspensiones.findById(suspensionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La suspensión no existe"));
        Category categoria = categories.findById(suspension.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        assertStaff(categoria.getEdition().getLeague().getId());
        if (!"ACTIVO".equals(suspension.getEstado())) {
            return;
        }
        String usuario = usuario();
        OffsetDateTime ahora = OffsetDateTime.now();
        suspension.setEstado("ELIMINADO");
        suspension.setFechaModificacion(ahora);
        suspension.setUsuarioModificacion(usuario);
        suspensiones.save(suspension);
        if ("MANUAL".equals(suspension.getOrigen())) {
            return;
        }
        if (exclusiones.findByIdJugadorAndIdJornadaOrigenAndMotivoAndEstado(
                suspension.getIdJugador(), suspension.getIdJornadaOrigen(), suspension.getMotivo(), "ACTIVO").isPresent()) {
            return;
        }
        SuspensionExclusion exclusion = new SuspensionExclusion();
        exclusion.setId(UUID.randomUUID());
        exclusion.setIdLiga(suspension.getIdLiga());
        exclusion.setIdCategoria(suspension.getIdCategoria());
        exclusion.setIdJugador(suspension.getIdJugador());
        exclusion.setIdJornadaOrigen(suspension.getIdJornadaOrigen());
        exclusion.setMotivo(suspension.getMotivo());
        exclusion.setEstado("ACTIVO");
        exclusion.setFechaCreacion(ahora);
        exclusion.setFechaModificacion(ahora);
        exclusion.setUsuarioCreacion(usuario);
        exclusion.setUsuarioModificacion(usuario);
        exclusiones.save(exclusion);
    }

    private void guardar(Category categoria, UUID jugadorId, UUID origenId, UUID inicioId, int cantidad, String motivo) {
        String usuario = usuario();
        OffsetDateTime ahora = OffsetDateTime.now();
        SuspensionJugador suspension = new SuspensionJugador();
        suspension.setId(UUID.randomUUID());
        suspension.setIdLiga(categoria.getEdition().getLeague().getId());
        suspension.setIdCategoria(categoria.getId());
        suspension.setIdJugador(jugadorId);
        suspension.setIdJornadaOrigen(origenId);
        suspension.setIdJornadaInicio(inicioId);
        suspension.setJornadas(cantidad);
        suspension.setMotivo(motivo);
        suspension.setOrigen("MANUAL");
        suspension.setEstado("ACTIVO");
        suspension.setFechaCreacion(ahora);
        suspension.setFechaModificacion(ahora);
        suspension.setUsuarioCreacion(usuario);
        suspension.setUsuarioModificacion(usuario);
        suspensiones.save(suspension);
    }

    private List<SuspensionesJugadorViews.OpcionView> opciones(Category categoria) {
        Regla regla = regla(categoria);
        List<SuspensionesJugadorViews.OpcionView> opciones = new ArrayList<>();
        if (regla.sancionActiva) {
            opciones.add(new SuspensionesJugadorViews.OpcionView("SANCION", "Sanción", regla.sancion, true));
        }
        if (regla.faltaGrave) {
            opciones.add(new SuspensionesJugadorViews.OpcionView("FALTA_GRAVE", "Eliminación del campeonato", 0, false));
        }
        return opciones;
    }

    private int jornadasPedidas(Integer jornadas) {
        if (jornadas == null || jornadas < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica las jornadas de la sanción");
        }
        return jornadas;
    }

    private int pendientes(List<SuspensionJugador> suspensionesJugador, List<Jornada> orden, int proxima) {
        if (proxima < 0) {
            return 0;
        }
        Map<UUID, Integer> indice = indice(orden);
        int total = 0;
        for (SuspensionJugador suspension : suspensionesJugador) {
            if ("FALTA_GRAVE".equals(suspension.getMotivo())) {
                total += orden.size() - proxima;
                continue;
            }
            Integer inicio = indice.get(suspension.getIdJornadaInicio());
            if (inicio == null) {
                continue;
            }
            for (int paso = 0; paso < suspension.getJornadas(); paso++) {
                int posicion = inicio + paso;
                if (posicion >= proxima && posicion < orden.size()) {
                    total++;
                }
            }
        }
        return total;
    }

    private Set<Integer> ocupadas(List<SuspensionJugador> activas, List<Jornada> orden) {
        Map<UUID, Integer> indice = indice(orden);
        Set<Integer> ocupadas = new HashSet<>();
        for (SuspensionJugador suspension : activas) {
            if ("FALTA_GRAVE".equals(suspension.getMotivo())) {
                for (int i = 0; i < orden.size(); i++) {
                    ocupadas.add(i);
                }
                continue;
            }
            Integer inicio = indice.get(suspension.getIdJornadaInicio());
            if (inicio == null) {
                continue;
            }
            for (int paso = 0; paso < suspension.getJornadas() && inicio + paso < orden.size(); paso++) {
                ocupadas.add(inicio + paso);
            }
        }
        return ocupadas;
    }

    private int libre(List<Jornada> orden, Set<Integer> ocupadas, int preferido, int cantidad) {
        for (int cursor = preferido; cursor + cantidad <= orden.size(); cursor++) {
            boolean libre = true;
            for (int paso = 0; paso < cantidad; paso++) {
                if (ocupadas.contains(cursor + paso)) {
                    libre = false;
                    break;
                }
            }
            if (libre) {
                return cursor;
            }
        }
        return -1;
    }

    private int inicio(List<Jornada> orden, String momento, LocalDate hoy) {
        if (PRIMERA_FECHA.equals(momento)) {
            for (int i = 0; i < orden.size(); i++) {
                LocalDate fecha = orden.get(i).getFecha();
                if (fecha != null && fecha.isAfter(hoy)) {
                    return i;
                }
            }
            return -1;
        }
        return proxima(orden, hoy);
    }

    private int proxima(List<Jornada> orden, LocalDate hoy) {
        for (int i = 0; i < orden.size(); i++) {
            LocalDate fecha = orden.get(i).getFecha();
            if (fecha != null && !fecha.isBefore(hoy)) {
                return i;
            }
        }
        return -1;
    }

    private int indiceDe(List<Jornada> orden, UUID jornadaId) {
        if (jornadaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige la jornada en la que inicia la sanción");
        }
        for (int i = 0; i < orden.size(); i++) {
            if (orden.get(i).getId().equals(jornadaId)) {
                return i;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La jornada no pertenece a esta categoría");
    }

    private Map<UUID, Integer> indice(List<Jornada> orden) {
        Map<UUID, Integer> indice = new HashMap<>();
        for (int i = 0; i < orden.size(); i++) {
            indice.put(orden.get(i).getId(), i);
        }
        return indice;
    }

    private Map<UUID, List<SuspensionJugador>> porJugador(UUID categoryId) {
        Map<UUID, List<SuspensionJugador>> porJugador = new HashMap<>();
        for (SuspensionJugador suspension : suspensiones.findByIdCategoriaAndEstado(categoryId, "ACTIVO")) {
            porJugador.computeIfAbsent(suspension.getIdJugador(), id -> new ArrayList<>()).add(suspension);
        }
        return porJugador;
    }

    private List<Jornada> ordenadas(UUID categoryId) {
        List<Jornada> orden = new ArrayList<>(jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoryId, "ACTIVO"));
        orden.sort(Comparator.comparing(Jornada::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Jornada::getNumero));
        return orden;
    }

    private SuspensionesJugadorViews.JornadaView vista(List<Jornada> orden, int proxima) {
        if (proxima < 0) {
            return null;
        }
        Jornada jornada = orden.get(proxima);
        return new SuspensionesJugadorViews.JornadaView(jornada.getId(), jornada.getNumero(), jornada.getNombre(), jornada.getFecha());
    }

    private int cantidad(Regla regla, String motivo) {
        if ("ROJA_DIRECTA".equals(motivo)) {
            return regla.roja;
        }
        if ("AMARILLAS".equals(motivo)) {
            return regla.amarillas;
        }
        if ("ROJAS_ACUMULADAS".equals(motivo)) {
            return regla.rojas;
        }
        if ("SANCION".equals(motivo)) {
            return regla.sancion;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suspensión no existe");
    }

    private String momento(Regla regla, String motivo) {
        if ("AMARILLAS".equals(motivo)) {
            return regla.momentoAmarillas;
        }
        if ("ROJAS_ACUMULADAS".equals(motivo)) {
            return regla.momentoRojas;
        }
        return regla.momentoRoja;
    }

    private Regla regla(Category categoria) {
        UUID ligaId = categoria.getEdition().getLeague().getId();
        ReglaSuspension regla = reglas.findByIdLigaAndEstado(ligaId, "ACTIVO").orElse(null);
        String momentoRoja = ajustes.findById(ligaId).map(LeagueSettings::getSuspensionTiming).orElse(JORNADA_SIGUIENTE);
        if (regla == null) {
            return new Regla(1, 1, 2, 1, momentoRoja, JORNADA_SIGUIENTE, JORNADA_SIGUIENTE,
                    true, true, true, true, true);
        }
        return new Regla(regla.getRojaJornadas(), regla.getAmarillasJornadas(), regla.getRojasJornadas(),
                regla.getSancionJornadas(), momentoRoja,
                regla.getAmarillasMomento() == null ? JORNADA_SIGUIENTE : regla.getAmarillasMomento(),
                regla.getRojasMomento() == null ? JORNADA_SIGUIENTE : regla.getRojasMomento(),
                "ACTIVO".equals(regla.getEstadoRoja()),
                "ACTIVO".equals(regla.getEstadoAmarillas()),
                "ACTIVO".equals(regla.getEstadoRojas()),
                "ACTIVO".equals(regla.getEstadoSancion()),
                "ACTIVO".equals(regla.getEstadoFaltaGrave()));
    }

    private boolean activa(Regla regla, String motivo) {
        if ("ROJA_DIRECTA".equals(motivo)) {
            return regla.rojaActiva;
        }
        if ("AMARILLAS".equals(motivo)) {
            return regla.amarillasActiva;
        }
        if ("ROJAS_ACUMULADAS".equals(motivo)) {
            return regla.rojasActiva;
        }
        if ("SANCION".equals(motivo)) {
            return regla.sancionActiva;
        }
        return false;
    }

    private String etiqueta(String motivo) {
        if ("ROJA_DIRECTA".equals(motivo)) {
            return "Tarjeta roja";
        }
        if ("AMARILLAS".equals(motivo)) {
            return "Acumulación de amarillas";
        }
        if ("ROJAS_ACUMULADAS".equals(motivo)) {
            return "Acumulación de rojas";
        }
        if ("SANCION".equals(motivo)) {
            return "Sanción";
        }
        if ("FALTA_GRAVE".equals(motivo)) {
            return "Eliminación del campeonato";
        }
        return motivo;
    }

    private String texto(Jornada jornada) {
        if (jornada == null) {
            return "";
        }
        String fecha = jornada.getFecha() == null ? "" : " · " + jornada.getFecha();
        return "Jornada " + jornada.getNumero() + fecha;
    }

    private String alias(Player jugador) {
        if (jugador.getAlias() != null && !jugador.getAlias().trim().isEmpty()) {
            return jugador.getAlias().trim();
        }
        return jugador.getName();
    }

    private Category categoria(UUID editionId, UUID categoryId) {
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una categoría");
        }
        Category categoria = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        if (editionId == null || !editionId.equals(categoria.getEdition().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a esta edición");
        }
        return categoria;
    }

    private Category categoriaDe(Player jugador, UUID categoryId) {
        if (categoryId == null || !categoryId.equals(jugador.getTeamEntry().getCategory().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no pertenece a esta categoría");
        }
        return jugador.getTeamEntry().getCategory();
    }

    private TeamEntry entrada(UUID categoryId, UUID teamId) {
        if (teamId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige un equipo");
        }
        return entradas.findByTeam_IdAndCategory_Id(teamId, categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no está inscrito en esta categoría"));
    }

    private Player jugador(UUID jugadorId) {
        return jugadores.findById(jugadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El jugador no existe"));
    }

    private void assertStaff(UUID leagueId) {
        AppUser user = users.findWithMembershipsByUsername(usuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
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

    private String usuario() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static final class Regla {
        private final int roja;
        private final int amarillas;
        private final int rojas;
        private final int sancion;
        private final String momentoRoja;
        private final String momentoAmarillas;
        private final String momentoRojas;
        private final boolean rojaActiva;
        private final boolean amarillasActiva;
        private final boolean rojasActiva;
        private final boolean sancionActiva;
        private final boolean faltaGrave;

        private Regla(int roja, int amarillas, int rojas, int sancion, String momentoRoja,
                      String momentoAmarillas, String momentoRojas, boolean rojaActiva, boolean amarillasActiva,
                      boolean rojasActiva, boolean sancionActiva, boolean faltaGrave) {
            this.roja = roja;
            this.amarillas = amarillas;
            this.rojas = rojas;
            this.sancion = sancion;
            this.momentoRoja = momentoRoja;
            this.momentoAmarillas = momentoAmarillas;
            this.momentoRojas = momentoRojas;
            this.rojaActiva = rojaActiva;
            this.amarillasActiva = amarillasActiva;
            this.rojasActiva = rojasActiva;
            this.sancionActiva = sancionActiva;
            this.faltaGrave = faltaGrave;
        }
    }
}
