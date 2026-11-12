package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ResultadoService {

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final CanchaRepository canchas;
    private final TeamRepository teams;
    private final AppUserRepository users;
    private final ArbitroRepository arbitros;
    private final PartidoArbitroRepository partidosArbitros;
    private final GolJugadorRepository golesJugadores;
    private final TarjetaJugadorRepository tarjetasJugadores;
    private final SancionJugadorRepository sancionesJugador;
    private final PartidoSancionRepository partidosSanciones;
    private final PartidoSancionDetalleRepository partidosSancionesDetalle;
    private final TeamEntryRepository entradas;
    private final PlayerRepository jugadores;

    public ResultadoService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                            PartidoRepository partidos, CanchaRepository canchas, TeamRepository teams,
                            AppUserRepository users, ArbitroRepository arbitros, PartidoArbitroRepository partidosArbitros,
                            GolJugadorRepository golesJugadores, TarjetaJugadorRepository tarjetasJugadores,
                            SancionJugadorRepository sancionesJugador, PartidoSancionRepository partidosSanciones,
                            PartidoSancionDetalleRepository partidosSancionesDetalle,
                            TeamEntryRepository entradas, PlayerRepository jugadores) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.canchas = canchas;
        this.teams = teams;
        this.users = users;
        this.arbitros = arbitros;
        this.partidosArbitros = partidosArbitros;
        this.golesJugadores = golesJugadores;
        this.tarjetasJugadores = tarjetasJugadores;
        this.sancionesJugador = sancionesJugador;
        this.partidosSanciones = partidosSanciones;
        this.partidosSancionesDetalle = partidosSancionesDetalle;
        this.entradas = entradas;
        this.jugadores = jugadores;
    }

    @Transactional(readOnly = true)
    public ResultadoViews.ResultadoFechaView fechaCercana(UUID editionId, UUID categoryId) {
        Edition edition = edition(editionId);
        assertRead(edition.getLeague().getId());
        Contexto contexto = contexto(edition, categoryId);
        LocalDate hoy = LocalDate.now();
        LocalDate mejor = null;
        long distancia = Long.MAX_VALUE;
        for (LocalDate dia : fechas(contexto)) {
            long actual = Math.abs(ChronoUnit.DAYS.between(hoy, dia));
            if (mejor == null || actual < distancia || (actual == distancia && dia.isAfter(mejor))) {
                mejor = dia;
                distancia = actual;
            }
        }
        return new ResultadoViews.ResultadoFechaView(mejor);
    }

    @Transactional(readOnly = true)
    public ResultadoViews.ResultadoFechaView fechaAnterior(UUID editionId, UUID categoryId, LocalDate desde) {
        return new ResultadoViews.ResultadoFechaView(vecina(editionId, categoryId, desde, false));
    }

    @Transactional(readOnly = true)
    public ResultadoViews.ResultadoFechaView proximaFecha(UUID editionId, UUID categoryId, LocalDate desde) {
        return new ResultadoViews.ResultadoFechaView(vecina(editionId, categoryId, desde, true));
    }

    @Transactional(readOnly = true)
    public ResultadoViews.ResultadoPanelView listar(UUID editionId, UUID categoryId, LocalDate fecha) {
        Edition edition = edition(editionId);
        assertRead(edition.getLeague().getId());
        Contexto contexto = contexto(edition, categoryId);
        List<ResultadoViews.ResultadoPartidoView> filas = new ArrayList<>();
        if (fecha != null) {
            for (Partido partido : contexto.partidos) {
                LocalDate dia = fechaEfectiva(partido, contexto);
                if (!fecha.equals(dia)) {
                    continue;
                }
                filas.add(vista(partido, contexto));
            }
        }
        filas.sort(Comparator.comparing((ResultadoViews.ResultadoPartidoView fila) -> fila.getHora() == null ? "" : fila.getHora())
                .thenComparing(ResultadoViews.ResultadoPartidoView::getCategoryName)
                .thenComparing(ResultadoViews.ResultadoPartidoView::getOrden));
        return new ResultadoViews.ResultadoPanelView(LocalDate.now(), filas);
    }

    @Transactional
    public ResultadoViews.ResultadoPartidoView guardar(UUID id, ResultadoViews.ResultadoGuardarRequest request) {
        Partido partido = partidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El partido no existe"));
        if (!"ACTIVO".equals(partido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El partido no está activo");
        }
        Category categoria = categories.findById(partido.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        assertMarcador(categoria.getEdition().getLeague().getId(), partido);
        if (partido.getIdEquipoLocal() == null || partido.getIdEquipoVisita() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El partido todavía no tiene los dos equipos");
        }
        partido.setGolesLocal(goles(request.getGolesLocal(), "local"));
        partido.setGolesVisita(goles(request.getGolesVisita(), "visita"));
        partido.setFechaModificacion(OffsetDateTime.now());
        partido.setUsuarioModificacion(usuario());
        partidos.save(partido);
        Edition edition = categoria.getEdition();
        Contexto contexto = contexto(edition, null);
        return vista(partido, contexto);
    }

    private LocalDate vecina(UUID editionId, UUID categoryId, LocalDate desde, boolean siguiente) {
        Edition edition = edition(editionId);
        assertRead(edition.getLeague().getId());
        Contexto contexto = contexto(edition, categoryId);
        LocalDate elegida = null;
        for (LocalDate dia : fechas(contexto)) {
            if (desde != null && (siguiente ? !dia.isAfter(desde) : !dia.isBefore(desde))) {
                continue;
            }
            if (elegida == null || (siguiente ? dia.isBefore(elegida) : dia.isAfter(elegida))) {
                elegida = dia;
            }
        }
        if (elegida == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    siguiente ? "No hay otra fecha de partidos" : "No hay una fecha anterior de partidos");
        }
        return elegida;
    }

    private Set<LocalDate> fechas(Contexto contexto) {
        Set<LocalDate> dias = new HashSet<>();
        for (Partido partido : contexto.partidos) {
            LocalDate dia = fechaEfectiva(partido, contexto);
            if (dia != null) {
                dias.add(dia);
            }
        }
        return dias;
    }

    private ResultadoViews.ResultadoPartidoView vista(Partido partido, Contexto contexto) {
        Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
        Category categoria = contexto.porCategoria.get(partido.getIdCategoria());
        String cancha = partido.getIdCancha() == null ? null : contexto.porCancha.get(partido.getIdCancha());
        return new ResultadoViews.ResultadoPartidoView(
                partido.getId(),
                categoria == null ? "" : categoria.getName(),
                jornada == null ? "" : jornada.getNombre(),
                cancha,
                fechaEfectiva(partido, contexto),
                textoHora(partido.getHora()),
                nombre(partido.getIdEquipoLocal(), contexto),
                nombre(partido.getIdEquipoVisita(), contexto),
                partido.getGolesLocal(),
                partido.getGolesVisita(),
                partido.getIdEquipoLocal() != null && partido.getIdEquipoVisita() != null,
                partido.getOrden());
    }

    private String nombre(UUID equipoId, Contexto contexto) {
        if (equipoId == null) {
            return "Por definir";
        }
        Team equipo = contexto.porEquipo.get(equipoId);
        if (equipo == null && !contexto.porEquipo.containsKey(equipoId)) {
            equipo = teams.findById(equipoId).orElse(null);
            contexto.porEquipo.put(equipoId, equipo);
        }
        return equipo == null ? "Por definir" : equipo.getName();
    }

    private Integer goles(Integer valor, String lado) {
        if (valor == null) {
            return null;
        }
        if (valor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los goles de " + lado + " deben ser cero o más");
        }
        return valor;
    }

    private String textoHora(LocalTime hora) {
        if (hora == null) {
            return null;
        }
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }

    private LocalDate fechaEfectiva(Partido partido, Contexto contexto) {
        if (partido.getFecha() != null) {
            return partido.getFecha();
        }
        Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
        return jornada == null ? null : jornada.getFecha();
    }

    @Transactional(readOnly = true)
    public ResultadoViews.DetalleView detalle(UUID partidoId) {
        Partido partido = partidoActivo(partidoId);
        Category categoria = categoria(partido);
        assertRead(categoria.getEdition().getLeague().getId());
        List<ResultadoViews.ArbitroOpcionView> opciones = new ArrayList<>();
        for (Arbitro arbitro : arbitros.buscar(categoria.getEdition().getLeague().getId(), "", "", "ACTIVO")) {
            opciones.add(new ResultadoViews.ArbitroOpcionView(arbitro.getId(), arbitro.getNombre(), arbitro.getFoto() != null));
        }
        List<ResultadoViews.ArbitroAsignadoView> asignados = new ArrayList<>();
        for (PartidoArbitro asignado : partidosArbitros.findByIdPartido(partido.getId())) {
            if (!"ACTIVO".equals(asignado.getEstado())) {
                continue;
            }
            asignados.add(new ResultadoViews.ArbitroAsignadoView(asignado.getIdArbitro(), asignado.isPito()));
        }
        List<ResultadoViews.GolCargadoView> goles = new ArrayList<>();
        for (GolJugador gol : golesJugadores.findByIdPartido(partido.getId())) {
            if (!"ACTIVO".equals(gol.getEstado())) {
                continue;
            }
            goles.add(new ResultadoViews.GolCargadoView(gol.getIdJugador(), gol.getCantidad()));
        }
        List<ResultadoViews.TarjetaCargadaView> tarjetas = new ArrayList<>();
        for (TarjetaJugador tarjeta : tarjetasJugadores.findByIdPartido(partido.getId())) {
            if (!"ACTIVO".equals(tarjeta.getEstado())) {
                continue;
            }
            tarjetas.add(new ResultadoViews.TarjetaCargadaView(
                    tarjeta.getIdJugador(), tarjeta.getAmarillas(), tarjeta.getRojas(), tarjeta.getObservacion()));
        }
        UUID ligaId = categoria.getEdition().getLeague().getId();
        List<ResultadoViews.SancionOpcionView> disponibles = new ArrayList<>();
        for (SancionJugador sancion : sancionesJugador.buscar(ligaId, "", "ACTIVO")) {
            disponibles.add(new ResultadoViews.SancionOpcionView(sancion.getId(), sancion.getDescripcion(), sancion.getCosto(), sancion.getEstado()));
        }
        List<ResultadoViews.SancionCargadaView> sanciones = new ArrayList<>();
        for (PartidoSancion cargada : partidosSanciones.findByIdPartido(partido.getId())) {
            if (!"ACTIVO".equals(cargada.getEstado())) {
                continue;
            }
            List<UUID> ids = new ArrayList<>();
            for (PartidoSancionDetalle detalleSancion : partidosSancionesDetalle.findByIdPartidoSancion(cargada.getId())) {
                if ("ACTIVO".equals(detalleSancion.getEstado())) {
                    ids.add(detalleSancion.getIdSancionJugador());
                }
            }
            sanciones.add(new ResultadoViews.SancionCargadaView(cargada.getIdJugador(), cargada.getObservacion(), ids));
        }
        Set<UUID> conocidas = new HashSet<>();
        for (ResultadoViews.SancionOpcionView opcion : disponibles) {
            conocidas.add(opcion.getId());
        }
        for (ResultadoViews.SancionCargadaView cargada : sanciones) {
            for (UUID sancionId : cargada.getSancionIds()) {
                if (!conocidas.add(sancionId)) {
                    continue;
                }
                sancionesJugador.findById(sancionId).ifPresent(sancion -> disponibles.add(
                        new ResultadoViews.SancionOpcionView(sancion.getId(), sancion.getDescripcion(), sancion.getCosto(), sancion.getEstado())));
            }
        }
        return new ResultadoViews.DetalleView(
                nombreEquipo(partido.getIdEquipoLocal()),
                nombreEquipo(partido.getIdEquipoVisita()),
                opcionEquipo(partido.getIdEquipoLocal()),
                opcionEquipo(partido.getIdEquipoVisita()),
                puedeEditar(ligaId, partido),
                opciones,
                asignados,
                goles,
                tarjetas,
                disponibles,
                sanciones);
    }

    @Transactional(readOnly = true)
    public List<ResultadoViews.JugadorPartidoView> jugadores(UUID partidoId, UUID teamId, String numero, String nombre) {
        Partido partido = partidoActivo(partidoId);
        Category categoria = categoria(partido);
        assertRead(categoria.getEdition().getLeague().getId());
        if (teamId == null || (!teamId.equals(partido.getIdEquipoLocal()) && !teamId.equals(partido.getIdEquipoVisita()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no juega este partido");
        }
        TeamEntry entrada = entradas.findByTeam_IdAndCategory_Id(teamId, partido.getIdCategoria()).orElse(null);
        if (entrada == null) {
            return new ArrayList<>();
        }
        String numeroFiltro = numero == null ? "" : numero.trim();
        String nombreFiltro = nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
        List<ResultadoViews.JugadorPartidoView> filas = new ArrayList<>();
        for (Player jugador : jugadores.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId())) {
            String camisa = String.valueOf(jugador.getNumeroCamisa());
            if (!numeroFiltro.isEmpty() && !camisa.contains(numeroFiltro)) {
                continue;
            }
            String alias = jugador.getAlias() == null ? "" : jugador.getAlias();
            String busqueda = (jugador.getName() + " " + alias).toLowerCase(Locale.ROOT);
            if (!nombreFiltro.isEmpty() && !busqueda.contains(nombreFiltro)) {
                continue;
            }
            filas.add(new ResultadoViews.JugadorPartidoView(
                    jugador.getId(),
                    jugador.getNumeroCamisa(),
                    alias,
                    jugador.getPhoto() != null,
                    jugador.getRotationDegrees()));
        }
        filas.sort(Comparator.comparingInt(ResultadoViews.JugadorPartidoView::getNumero).thenComparing(ResultadoViews.JugadorPartidoView::getAlias));
        return filas;
    }

    @Transactional
    public ResultadoViews.DetalleView guardarDetalle(UUID partidoId, ResultadoViews.DetalleGuardarRequest request) {
        Partido partido = partidoActivo(partidoId);
        Category categoria = categoria(partido);
        assertMarcador(categoria.getEdition().getLeague().getId(), partido);
        guardarArbitros(partido, categoria.getEdition().getLeague().getId(), request.getArbitros());
        guardarGoles(partido, request.getGoles());
        guardarTarjetas(partido, request.getTarjetas());
        guardarSanciones(partido, categoria.getEdition().getLeague().getId(), request.getSanciones());
        return detalle(partidoId);
    }

    private void guardarArbitros(Partido partido, UUID leagueId, List<ResultadoViews.ArbitroAsignadoRequest> pedidos) {
        Map<UUID, PartidoArbitro> existentes = new HashMap<>();
        for (PartidoArbitro asignado : partidosArbitros.findByIdPartido(partido.getId())) {
            existentes.put(asignado.getIdArbitro(), asignado);
        }
        Set<UUID> conservados = new HashSet<>();
        String usuario = usuario();
        if (pedidos != null) {
            for (ResultadoViews.ArbitroAsignadoRequest pedido : pedidos) {
                if (pedido.getArbitroId() == null || !conservados.add(pedido.getArbitroId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El árbitro está repetido");
                }
                Arbitro arbitro = arbitros.findById(pedido.getArbitroId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El árbitro no existe"));
                if (!leagueId.equals(arbitro.getIdLiga()) || !"ACTIVO".equals(arbitro.getEstado())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El árbitro debe estar activo en la liga");
                }
                PartidoArbitro asignado = existentes.get(arbitro.getId());
                boolean nuevo = asignado == null;
                if (nuevo) {
                    asignado = new PartidoArbitro();
                    asignado.setId(UUID.randomUUID());
                    asignado.setIdPartido(partido.getId());
                    asignado.setIdArbitro(arbitro.getId());
                    asignado.setFechaCreacion(OffsetDateTime.now());
                    asignado.setUsuarioCreacion(usuario);
                }
                asignado.setPito(pedido.isPito());
                asignado.setEstado("ACTIVO");
                asignado.setFechaModificacion(OffsetDateTime.now());
                asignado.setUsuarioModificacion(usuario);
                partidosArbitros.save(asignado);
            }
        }
        for (PartidoArbitro asignado : existentes.values()) {
            if (conservados.contains(asignado.getIdArbitro()) || "ELIMINADO".equals(asignado.getEstado())) {
                continue;
            }
            asignado.setEstado("ELIMINADO");
            asignado.setFechaModificacion(OffsetDateTime.now());
            asignado.setUsuarioModificacion(usuario);
            partidosArbitros.save(asignado);
        }
    }

    private void guardarGoles(Partido partido, List<ResultadoViews.GolJugadorRequest> pedidos) {
        Map<UUID, GolJugador> existentes = new HashMap<>();
        for (GolJugador gol : golesJugadores.findByIdPartido(partido.getId())) {
            existentes.put(gol.getIdJugador(), gol);
        }
        Set<UUID> equipos = new HashSet<>();
        if (partido.getIdEquipoLocal() != null) {
            equipos.add(partido.getIdEquipoLocal());
        }
        if (partido.getIdEquipoVisita() != null) {
            equipos.add(partido.getIdEquipoVisita());
        }
        Set<UUID> conservados = new HashSet<>();
        String usuario = usuario();
        if (pedidos != null) {
            for (ResultadoViews.GolJugadorRequest pedido : pedidos) {
                int cantidad = pedido.getCantidad() == null ? 0 : pedido.getCantidad();
                if (cantidad < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de goles debe ser cero o más");
                }
                if (cantidad == 0 || pedido.getJugadorId() == null) {
                    continue;
                }
                if (!conservados.add(pedido.getJugadorId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador está repetido");
                }
                Player jugador = jugadores.findById(pedido.getJugadorId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no existe"));
                UUID equipo = jugador.getTeamEntry().getTeam().getId();
                if (!equipos.contains(equipo) || !partido.getIdCategoria().equals(jugador.getTeamEntry().getCategory().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no pertenece a los equipos del partido");
                }
                GolJugador gol = existentes.get(jugador.getId());
                boolean nuevo = gol == null;
                if (nuevo) {
                    gol = new GolJugador();
                    gol.setId(UUID.randomUUID());
                    gol.setIdPartido(partido.getId());
                    gol.setIdJugador(jugador.getId());
                    gol.setFechaCreacion(OffsetDateTime.now());
                    gol.setUsuarioCreacion(usuario);
                }
                gol.setCantidad(cantidad);
                gol.setEstado("ACTIVO");
                gol.setFechaModificacion(OffsetDateTime.now());
                gol.setUsuarioModificacion(usuario);
                golesJugadores.save(gol);
            }
        }
        for (GolJugador gol : existentes.values()) {
            if (conservados.contains(gol.getIdJugador()) || "ELIMINADO".equals(gol.getEstado())) {
                continue;
            }
            gol.setEstado("ELIMINADO");
            gol.setFechaModificacion(OffsetDateTime.now());
            gol.setUsuarioModificacion(usuario);
            golesJugadores.save(gol);
        }
    }

    private void guardarTarjetas(Partido partido, List<ResultadoViews.TarjetaJugadorRequest> pedidos) {
        Map<UUID, TarjetaJugador> existentes = new HashMap<>();
        for (TarjetaJugador tarjeta : tarjetasJugadores.findByIdPartido(partido.getId())) {
            existentes.put(tarjeta.getIdJugador(), tarjeta);
        }
        Set<UUID> equipos = new HashSet<>();
        if (partido.getIdEquipoLocal() != null) {
            equipos.add(partido.getIdEquipoLocal());
        }
        if (partido.getIdEquipoVisita() != null) {
            equipos.add(partido.getIdEquipoVisita());
        }
        LocalDate fecha = fechaDelPartido(partido);
        Set<UUID> conservados = new HashSet<>();
        String usuario = usuario();
        if (pedidos != null) {
            for (ResultadoViews.TarjetaJugadorRequest pedido : pedidos) {
                Integer amarillas = cantidadTarjeta(pedido.getAmarillas(), true);
                Integer rojas = cantidadTarjeta(pedido.getRojas(), false);
                String observacion = observacion(pedido.getObservacion());
                if (pedido.getJugadorId() == null || (amarillas == null && rojas == null && observacion == null)) {
                    continue;
                }
                if (!conservados.add(pedido.getJugadorId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador está repetido");
                }
                Player jugador = jugadores.findById(pedido.getJugadorId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no existe"));
                UUID equipo = jugador.getTeamEntry().getTeam().getId();
                if (!equipos.contains(equipo) || !partido.getIdCategoria().equals(jugador.getTeamEntry().getCategory().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no pertenece a los equipos del partido");
                }
                TarjetaJugador tarjeta = existentes.get(jugador.getId());
                boolean nuevo = tarjeta == null;
                if (nuevo) {
                    tarjeta = new TarjetaJugador();
                    tarjeta.setId(UUID.randomUUID());
                    tarjeta.setIdPartido(partido.getId());
                    tarjeta.setIdJugador(jugador.getId());
                    tarjeta.setFechaCreacion(OffsetDateTime.now());
                    tarjeta.setUsuarioCreacion(usuario);
                }
                tarjeta.setIdJornada(partido.getIdJornada());
                tarjeta.setFecha(fecha);
                tarjeta.setAmarillas(amarillas);
                tarjeta.setRojas(rojas);
                tarjeta.setObservacion(observacion);
                tarjeta.setEstado("ACTIVO");
                tarjeta.setFechaModificacion(OffsetDateTime.now());
                tarjeta.setUsuarioModificacion(usuario);
                tarjetasJugadores.save(tarjeta);
            }
        }
        for (TarjetaJugador tarjeta : existentes.values()) {
            if (conservados.contains(tarjeta.getIdJugador()) || "ELIMINADO".equals(tarjeta.getEstado())) {
                continue;
            }
            tarjeta.setEstado("ELIMINADO");
            tarjeta.setFechaModificacion(OffsetDateTime.now());
            tarjeta.setUsuarioModificacion(usuario);
            tarjetasJugadores.save(tarjeta);
        }
    }

    private Integer cantidadTarjeta(Integer valor, boolean amarilla) {
        if (valor == null || valor == 0) {
            return null;
        }
        if (amarilla && (valor == 1 || valor == 2)) {
            return valor;
        }
        if (!amarilla && valor == 1) {
            return valor;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                amarilla ? "La tarjeta amarilla solo acepta 1 o 2" : "La tarjeta roja solo acepta 1");
    }

    private String observacion(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String texto = valor.trim();
        if (texto.length() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La observación es demasiado larga");
        }
        return texto;
    }

    private void guardarSanciones(Partido partido, UUID leagueId, List<ResultadoViews.SancionPartidoRequest> pedidos) {
        Map<UUID, PartidoSancion> existentes = new HashMap<>();
        for (PartidoSancion sancion : partidosSanciones.findByIdPartido(partido.getId())) {
            existentes.put(sancion.getIdJugador(), sancion);
        }
        Set<UUID> equipos = new HashSet<>();
        if (partido.getIdEquipoLocal() != null) {
            equipos.add(partido.getIdEquipoLocal());
        }
        if (partido.getIdEquipoVisita() != null) {
            equipos.add(partido.getIdEquipoVisita());
        }
        LocalDate fecha = fechaDelPartido(partido);
        Set<UUID> conservados = new HashSet<>();
        String usuario = usuario();
        if (pedidos != null) {
            for (ResultadoViews.SancionPartidoRequest pedido : pedidos) {
                String observacion = observacion(pedido.getObservacion());
                List<UUID> pedidas = pedido.getSancionIds() == null ? new ArrayList<>() : pedido.getSancionIds();
                if (pedido.getJugadorId() == null || (pedidas.isEmpty() && observacion == null)) {
                    continue;
                }
                if (!conservados.add(pedido.getJugadorId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador está repetido");
                }
                Player jugador = jugadores.findById(pedido.getJugadorId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no existe"));
                UUID equipo = jugador.getTeamEntry().getTeam().getId();
                if (!equipos.contains(equipo) || !partido.getIdCategoria().equals(jugador.getTeamEntry().getCategory().getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no pertenece a los equipos del partido");
                }
                Set<UUID> unicas = new HashSet<>();
                for (UUID sancionId : pedidas) {
                    if (sancionId == null || !unicas.add(sancionId)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sanción está repetida");
                    }
                    SancionJugador catalogo = sancionesJugador.findById(sancionId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sanción no existe"));
                    if (!leagueId.equals(catalogo.getIdLiga()) || !"ACTIVO".equals(catalogo.getEstado())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sanción debe estar activa en la liga");
                    }
                }
                PartidoSancion sancion = existentes.get(jugador.getId());
                boolean nuevo = sancion == null;
                if (nuevo) {
                    sancion = new PartidoSancion();
                    sancion.setId(UUID.randomUUID());
                    sancion.setIdPartido(partido.getId());
                    sancion.setIdJugador(jugador.getId());
                    sancion.setFechaCreacion(OffsetDateTime.now());
                    sancion.setUsuarioCreacion(usuario);
                }
                sancion.setIdJornada(partido.getIdJornada());
                sancion.setFecha(fecha);
                sancion.setObservacion(observacion);
                sancion.setEstado("ACTIVO");
                sancion.setFechaModificacion(OffsetDateTime.now());
                sancion.setUsuarioModificacion(usuario);
                partidosSanciones.save(sancion);
                sincronizarDetalle(sancion, unicas, usuario);
            }
        }
        for (PartidoSancion sancion : existentes.values()) {
            if (conservados.contains(sancion.getIdJugador()) || "ELIMINADO".equals(sancion.getEstado())) {
                continue;
            }
            sancion.setEstado("ELIMINADO");
            sancion.setFechaModificacion(OffsetDateTime.now());
            sancion.setUsuarioModificacion(usuario);
            partidosSanciones.save(sancion);
            sincronizarDetalle(sancion, new HashSet<>(), usuario);
        }
    }

    private void sincronizarDetalle(PartidoSancion sancion, Set<UUID> pedidas, String usuario) {
        Map<UUID, PartidoSancionDetalle> existentes = new HashMap<>();
        for (PartidoSancionDetalle detalle : partidosSancionesDetalle.findByIdPartidoSancion(sancion.getId())) {
            existentes.put(detalle.getIdSancionJugador(), detalle);
        }
        for (UUID sancionId : pedidas) {
            PartidoSancionDetalle detalle = existentes.get(sancionId);
            boolean nuevo = detalle == null;
            if (nuevo) {
                detalle = new PartidoSancionDetalle();
                detalle.setId(UUID.randomUUID());
                detalle.setIdPartidoSancion(sancion.getId());
                detalle.setIdSancionJugador(sancionId);
                detalle.setFechaCreacion(OffsetDateTime.now());
                detalle.setUsuarioCreacion(usuario);
            }
            detalle.setEstado("ACTIVO");
            detalle.setFechaModificacion(OffsetDateTime.now());
            detalle.setUsuarioModificacion(usuario);
            partidosSancionesDetalle.save(detalle);
        }
        for (PartidoSancionDetalle detalle : existentes.values()) {
            if (pedidas.contains(detalle.getIdSancionJugador()) || "ELIMINADO".equals(detalle.getEstado())) {
                continue;
            }
            detalle.setEstado("ELIMINADO");
            detalle.setFechaModificacion(OffsetDateTime.now());
            detalle.setUsuarioModificacion(usuario);
            partidosSancionesDetalle.save(detalle);
        }
    }

    private LocalDate fechaDelPartido(Partido partido) {
        if (partido.getFecha() != null) {
            return partido.getFecha();
        }
        return jornadas.findById(partido.getIdJornada()).map(Jornada::getFecha).orElse(null);
    }

    private Partido partidoActivo(UUID id) {
        Partido partido = partidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El partido no existe"));
        if (!"ACTIVO".equals(partido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El partido no está activo");
        }
        return partido;
    }

    private Category categoria(Partido partido) {
        return categories.findById(partido.getIdCategoria())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
    }

    private String nombreEquipo(UUID id) {
        if (id == null) {
            return "Por definir";
        }
        return teams.findById(id).map(Team::getName).orElse("Por definir");
    }

    private ResultadoViews.EquipoOpcionView opcionEquipo(UUID id) {
        if (id == null) {
            return null;
        }
        return new ResultadoViews.EquipoOpcionView(id, nombreEquipo(id));
    }

    private boolean puedeEditar(UUID leagueId, Partido partido) {
        AppUser user = actual();
        if (user.isMaster()) {
            return true;
        }
        boolean admin = user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId)
                        && "ADMINISTRATIVO".equals(membership.getRole()));
        if (admin) {
            return true;
        }
        boolean asistente = user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId)
                        && "ASISTENTE".equals(membership.getRole()));
        Jornada jornada = jornadas.findById(partido.getIdJornada()).orElse(null);
        LocalDate dia = partido.getFecha() != null ? partido.getFecha() : jornada == null ? null : jornada.getFecha();
        return asistente && LocalDate.now().equals(dia);
    }

    private void assertMarcador(UUID leagueId, Partido partido) {
        if (!puedeEditar(leagueId, partido)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este marcador");
        }
    }

    private Contexto contexto(Edition edition, UUID categoryId) {
        Contexto contexto = new Contexto();
        List<UUID> categorias = new ArrayList<>();
        for (Category categoria : categories.findByEdition_IdOrderByNameAsc(edition.getId())) {
            contexto.porCategoria.put(categoria.getId(), categoria);
            contexto.categoriasEdicion.add(categoria.getId());
            categorias.add(categoria.getId());
        }
        if (categoryId != null && !contexto.categoriasEdicion.contains(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a esta edición");
        }
        if (!categorias.isEmpty()) {
            for (Jornada jornada : jornadas.findByIdCategoriaInAndEstado(categorias, "ACTIVO")) {
                contexto.porJornada.put(jornada.getId(), jornada);
            }
            for (Partido partido : partidos.findByIdCategoriaInAndEstado(categorias, "ACTIVO")) {
                if (categoryId != null && !categoryId.equals(partido.getIdCategoria())) {
                    continue;
                }
                Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
                if (jornada == null || !"ACTIVO".equals(jornada.getEstado())) {
                    continue;
                }
                contexto.partidos.add(partido);
            }
        }
        for (Cancha cancha : canchas.buscar(edition.getLeague().getId(), "", "")) {
            contexto.porCancha.put(cancha.getId(), cancha.getDescripcion());
        }
        return contexto;
    }

    private Edition edition(UUID id) {
        return editions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
    }

    private String usuario() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private AppUser actual() {
        return users.findWithMembershipsByUsername(usuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void assertRead(UUID leagueId) {
        AppUser user = actual();
        boolean miembro = user.isMaster() || user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId));
        if (!miembro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta liga");
        }
    }

    private static final class Contexto {
        private final Map<UUID, Category> porCategoria = new HashMap<>();
        private final Set<UUID> categoriasEdicion = new HashSet<>();
        private final Map<UUID, Jornada> porJornada = new HashMap<>();
        private final Map<UUID, String> porCancha = new HashMap<>();
        private final Map<UUID, Team> porEquipo = new HashMap<>();
        private final List<Partido> partidos = new ArrayList<>();
    }
}
