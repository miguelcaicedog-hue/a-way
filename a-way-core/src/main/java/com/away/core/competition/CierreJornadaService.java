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
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CierreJornadaService {

    private static final String ABIERTA = "ABIERTA";
    private static final String CERRADA = "CERRADA";
    private static final String JORNADA_SIGUIENTE = "NEXT_MATCHDAY";
    private static final String PRIMERA_FECHA = "FIRST_MATCHDAY_OF_DAY";

    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final TarjetaJugadorRepository tarjetas;
    private final SuspensionJugadorRepository suspensiones;
    private final SuspensionExclusionRepository exclusiones;
    private final ReglaSuspensionRepository reglas;
    private final LeagueSettingsRepository ajustes;
    private final AppUserRepository users;

    public CierreJornadaService(CategoryRepository categories, JornadaRepository jornadas, PartidoRepository partidos,
                                TarjetaJugadorRepository tarjetas, SuspensionJugadorRepository suspensiones,
                                SuspensionExclusionRepository exclusiones, ReglaSuspensionRepository reglas,
                                LeagueSettingsRepository ajustes, AppUserRepository users) {
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.tarjetas = tarjetas;
        this.suspensiones = suspensiones;
        this.exclusiones = exclusiones;
        this.reglas = reglas;
        this.ajustes = ajustes;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public boolean fechaCerrada(UUID categoryId, LocalDate fecha) {
        List<Jornada> delDia = jornadasDeFecha(categoryId, fecha);
        if (delDia.isEmpty()) {
            return false;
        }
        for (Jornada jornada : delDia) {
            if (!CERRADA.equals(jornada.getEstadoCierre())) {
                return false;
            }
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean jornadaCerrada(UUID jornadaId) {
        return jornadas.findById(jornadaId).map(jornada -> CERRADA.equals(jornada.getEstadoCierre())).orElse(false);
    }

    @Transactional
    public boolean cerrar(UUID editionId, UUID categoryId, LocalDate fecha) {
        return cambiar(editionId, categoryId, fecha, true);
    }

    @Transactional
    public boolean abrir(UUID editionId, UUID categoryId, LocalDate fecha) {
        return cambiar(editionId, categoryId, fecha, false);
    }

    @Transactional(readOnly = true)
    public Set<UUID> suspendidos(UUID categoryId, UUID jornadaId, Collection<UUID> jugadores) {
        Set<UUID> pedidos = new HashSet<>();
        if (jugadores != null) {
            pedidos.addAll(jugadores);
        }
        Set<UUID> resultado = new HashSet<>();
        if (pedidos.isEmpty() || jornadaId == null) {
            return resultado;
        }
        List<Jornada> orden = ordenadas(categoryId);
        Map<UUID, Integer> indice = indice(orden);
        Integer aqui = indice.get(jornadaId);
        if (aqui == null) {
            return resultado;
        }
        for (SuspensionJugador suspension : suspensiones.findByIdCategoriaAndEstado(categoryId, "ACTIVO")) {
            if (!pedidos.contains(suspension.getIdJugador())) {
                continue;
            }
            if ("FALTA_GRAVE".equals(suspension.getMotivo())) {
                resultado.add(suspension.getIdJugador());
                continue;
            }
            Integer inicio = indice.get(suspension.getIdJornadaInicio());
            if (inicio != null && aqui >= inicio && aqui < inicio + suspension.getJornadas()) {
                resultado.add(suspension.getIdJugador());
            }
        }
        return resultado;
    }

    public void assertPuedeRegistrar(UUID categoryId, UUID jornadaId, UUID jugadorId) {
        if (suspendidos(categoryId, jornadaId, List.of(jugadorId)).contains(jugadorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador está suspendido en esta jornada");
        }
    }

    private boolean cambiar(UUID editionId, UUID categoryId, LocalDate fecha, boolean cerrar) {
        if (categoryId == null || fecha == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una categoría y una fecha");
        }
        Category categoria = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        if (!editionId.equals(categoria.getEdition().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a esta edición");
        }
        assertWrite(categoria.getEdition().getLeague().getId());
        List<Jornada> delDia = jornadasDeFecha(categoryId, fecha);
        if (delDia.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay jornadas en esta fecha");
        }
        String usuario = usuario();
        OffsetDateTime ahora = OffsetDateTime.now();
        for (Jornada jornada : delDia) {
            jornada.setEstadoCierre(cerrar ? CERRADA : ABIERTA);
            jornada.setFechaModificacion(ahora);
            jornada.setUsuarioModificacion(usuario);
            jornadas.save(jornada);
        }
        recalcular(categoria, usuario);
        return cerrar;
    }

    private void recalcular(Category categoria, String usuario) {
        List<Jornada> orden = ordenadas(categoria.getId());
        Map<UUID, Integer> indice = indice(orden);
        Map<UUID, Set<Integer>> manuales = new HashMap<>();
        for (SuspensionJugador anterior : suspensiones.findByIdCategoriaAndEstado(categoria.getId(), "ACTIVO")) {
            if ("MANUAL".equals(anterior.getOrigen())) {
                ocupar(manuales, anterior, orden, indice);
                continue;
            }
            anterior.setEstado("ELIMINADO");
            anterior.setFechaModificacion(OffsetDateTime.now());
            anterior.setUsuarioModificacion(usuario);
            suspensiones.save(anterior);
        }
        Set<String> excluidas = new HashSet<>();
        for (SuspensionExclusion exclusion : exclusiones.findByIdCategoriaAndEstado(categoria.getId(), "ACTIVO")) {
            excluidas.add(clave(exclusion.getIdJugador(), exclusion.getIdJornadaOrigen(), exclusion.getMotivo()));
        }
        Map<UUID, Partido> porPartido = new HashMap<>();
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoria.getId(), "ACTIVO")) {
            porPartido.put(partido.getId(), partido);
        }
        ReglaSuspension regla = reglas.findByIdLigaAndEstado(categoria.getEdition().getLeague().getId(), "ACTIVO").orElse(null);
        boolean rojaActiva = regla == null || "ACTIVO".equals(regla.getEstadoRoja());
        boolean amarillasActivas = regla == null || "ACTIVO".equals(regla.getEstadoAmarillas());
        boolean rojasActivas = regla == null || "ACTIVO".equals(regla.getEstadoRojas());
        int rojaJornadas = !rojaActiva ? 0 : regla == null ? 1 : regla.getRojaJornadas();
        int amarillasUmbral = !amarillasActivas ? 0 : regla == null ? 3 : regla.getAmarillasAcumuladas();
        int amarillasJornadas = !amarillasActivas ? 0 : regla == null ? 1 : regla.getAmarillasJornadas();
        int rojasUmbral = !rojasActivas ? 0 : regla == null ? 2 : regla.getRojasAcumuladas();
        int rojasJornadas = !rojasActivas ? 0 : regla == null ? 2 : regla.getRojasJornadas();
        String momentoRoja = ajustes.findById(categoria.getEdition().getLeague().getId())
                .map(LeagueSettings::getSuspensionTiming).orElse(JORNADA_SIGUIENTE);
        String momentoAmarillas = regla == null || regla.getAmarillasMomento() == null ? JORNADA_SIGUIENTE : regla.getAmarillasMomento();
        String momentoRojas = regla == null || regla.getRojasMomento() == null ? JORNADA_SIGUIENTE : regla.getRojasMomento();

        Map<UUID, List<TarjetaJugador>> porJugador = new HashMap<>();
        for (TarjetaJugador tarjeta : tarjetas.activasDeCategoria(categoria.getId())) {
            Integer posicion = indice.get(tarjeta.getIdJornada());
            if (posicion == null || !CERRADA.equals(orden.get(posicion).getEstadoCierre())) {
                continue;
            }
            porJugador.computeIfAbsent(tarjeta.getIdJugador(), id -> new ArrayList<>()).add(tarjeta);
        }
        for (Map.Entry<UUID, List<TarjetaJugador>> entrada : porJugador.entrySet()) {
            entrada.getValue().sort(Comparator
                    .comparingInt((TarjetaJugador tarjeta) -> indice.getOrDefault(tarjeta.getIdJornada(), 0))
                    .thenComparingInt(tarjeta -> ordenPartido(tarjeta, porPartido)));
            aplicar(categoria, entrada.getKey(), entrada.getValue(), orden, indice, usuario,
                    rojaJornadas, momentoRoja, amarillasUmbral, amarillasJornadas, momentoAmarillas,
                    rojasUmbral, rojasJornadas, momentoRojas,
                    new HashSet<>(manuales.getOrDefault(entrada.getKey(), Collections.emptySet())), excluidas);
        }
    }

    private void aplicar(Category categoria, UUID jugadorId, List<TarjetaJugador> tarjetasJugador, List<Jornada> orden,
                         Map<UUID, Integer> indice, String usuario,
                         int rojaJornadas, String momentoRoja, int amarillasUmbral, int amarillasJornadas, String momentoAmarillas,
                         int rojasUmbral, int rojasJornadas, String momentoRojas, Set<Integer> ocupadas, Set<String> excluidas) {
        int amarillasPendientes = 0;
        int rojasPendientes = 0;
        for (TarjetaJugador tarjeta : tarjetasJugador) {
            int origen = indice.get(tarjeta.getIdJornada());
            int amarillas = tarjeta.getAmarillas() == null ? 0 : tarjeta.getAmarillas();
            int rojas = tarjeta.getRojas() == null ? 0 : tarjeta.getRojas();
            boolean cuentaAmarillas = !(amarillas == 2 && rojas >= 1);
            if (rojas >= 1) {
                colocar(categoria, jugadorId, orden, ocupadas, origen, inicio(orden, origen, momentoRoja), rojaJornadas, "ROJA_DIRECTA", usuario, excluidas);
                rojasPendientes += rojas;
                while (rojasUmbral > 0 && rojasPendientes >= rojasUmbral) {
                    colocar(categoria, jugadorId, orden, ocupadas, origen, inicio(orden, origen, momentoRojas), rojasJornadas, "ROJAS_ACUMULADAS", usuario, excluidas);
                    rojasPendientes -= rojasUmbral;
                }
            }
            if (cuentaAmarillas && amarillas > 0) {
                amarillasPendientes += amarillas;
                while (amarillasUmbral > 0 && amarillasPendientes >= amarillasUmbral) {
                    colocar(categoria, jugadorId, orden, ocupadas, origen, inicio(orden, origen, momentoAmarillas), amarillasJornadas, "AMARILLAS", usuario, excluidas);
                    amarillasPendientes -= amarillasUmbral;
                }
            }
        }
    }

    private void colocar(Category categoria, UUID jugadorId, List<Jornada> orden, Set<Integer> ocupadas, int origen,
                         int preferido, int cantidad, String motivo, String usuario, Set<String> excluidas) {
        if (cantidad <= 0 || preferido < 0 || origen < 0 || origen >= orden.size()) {
            return;
        }
        if (excluidas.contains(clave(jugadorId, orden.get(origen).getId(), motivo))) {
            return;
        }
        for (int cursor = preferido; cursor + cantidad <= orden.size(); cursor++) {
            boolean libre = true;
            for (int paso = 0; paso < cantidad; paso++) {
                if (ocupadas.contains(cursor + paso)) {
                    libre = false;
                    break;
                }
            }
            if (!libre) {
                continue;
            }
            for (int paso = 0; paso < cantidad; paso++) {
                ocupadas.add(cursor + paso);
            }
            SuspensionJugador suspension = new SuspensionJugador();
            suspension.setId(UUID.randomUUID());
            suspension.setIdLiga(categoria.getEdition().getLeague().getId());
            suspension.setIdCategoria(categoria.getId());
            suspension.setIdJugador(jugadorId);
            suspension.setIdJornadaOrigen(orden.get(origen).getId());
            suspension.setIdJornadaInicio(orden.get(cursor).getId());
            suspension.setJornadas(cantidad);
            suspension.setMotivo(motivo);
            suspension.setOrigen("CIERRE");
            suspension.setEstado("ACTIVO");
            OffsetDateTime ahora = OffsetDateTime.now();
            suspension.setFechaCreacion(ahora);
            suspension.setFechaModificacion(ahora);
            suspension.setUsuarioCreacion(usuario);
            suspension.setUsuarioModificacion(usuario);
            suspensiones.save(suspension);
            return;
        }
    }

    private int inicio(List<Jornada> orden, int origen, String momento) {
        if (PRIMERA_FECHA.equals(momento)) {
            LocalDate fecha = orden.get(origen).getFecha();
            if (fecha != null) {
                for (int i = origen + 1; i < orden.size(); i++) {
                    LocalDate otra = orden.get(i).getFecha();
                    if (otra != null && otra.isAfter(fecha)) {
                        return i;
                    }
                }
                return -1;
            }
        }
        return origen + 1 < orden.size() ? origen + 1 : -1;
    }

    private int ordenPartido(TarjetaJugador tarjeta, Map<UUID, Partido> porPartido) {
        Partido partido = porPartido.get(tarjeta.getIdPartido());
        return partido == null ? 0 : partido.getOrden();
    }

    private List<Jornada> jornadasDeFecha(UUID categoryId, LocalDate fecha) {
        Map<UUID, Jornada> porId = new HashMap<>();
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoryId, "ACTIVO")) {
            porId.put(jornada.getId(), jornada);
        }
        Set<UUID> elegidas = new HashSet<>();
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            Jornada jornada = porId.get(partido.getIdJornada());
            if (jornada == null) {
                continue;
            }
            LocalDate dia = partido.getFecha() != null ? partido.getFecha() : jornada.getFecha();
            if (fecha.equals(dia)) {
                elegidas.add(jornada.getId());
            }
        }
        List<Jornada> delDia = new ArrayList<>();
        for (UUID id : elegidas) {
            delDia.add(porId.get(id));
        }
        return delDia;
    }

    private void ocupar(Map<UUID, Set<Integer>> manuales, SuspensionJugador suspension, List<Jornada> orden, Map<UUID, Integer> indice) {
        Set<Integer> ocupadas = manuales.computeIfAbsent(suspension.getIdJugador(), id -> new HashSet<>());
        if ("FALTA_GRAVE".equals(suspension.getMotivo())) {
            for (int i = 0; i < orden.size(); i++) {
                ocupadas.add(i);
            }
            return;
        }
        Integer inicio = indice.get(suspension.getIdJornadaInicio());
        if (inicio == null) {
            return;
        }
        for (int paso = 0; paso < suspension.getJornadas() && inicio + paso < orden.size(); paso++) {
            ocupadas.add(inicio + paso);
        }
    }

    private String clave(UUID jugadorId, UUID jornadaOrigenId, String motivo) {
        return jugadorId + "|" + jornadaOrigenId + "|" + motivo;
    }

    private List<Jornada> ordenadas(UUID categoryId) {
        List<Jornada> orden = new ArrayList<>(jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoryId, "ACTIVO"));
        orden.sort(Comparator.comparing(Jornada::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Jornada::getNumero));
        return orden;
    }

    private Map<UUID, Integer> indice(List<Jornada> orden) {
        Map<UUID, Integer> indice = new HashMap<>();
        for (int i = 0; i < orden.size(); i++) {
            indice.put(orden.get(i).getId(), i);
        }
        return indice;
    }

    private String usuario() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private AppUser actual() {
        return users.findWithMembershipsByUsername(usuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void assertWrite(UUID leagueId) {
        AppUser user = actual();
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
}
