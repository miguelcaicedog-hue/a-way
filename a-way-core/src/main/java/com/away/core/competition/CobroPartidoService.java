package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
public class CobroPartidoService {

    private static final int AMARILLO_R = 0xF5;
    private static final int AMARILLO_G = 0xC5;
    private static final int AMARILLO_B = 0x18;
    private static final int ROJO_R = 0xC6;
    private static final int ROJO_G = 0x28;
    private static final int ROJO_B = 0x28;

    private final PartidoRepository partidos;
    private final JornadaRepository jornadas;
    private final CategoryRepository categories;
    private final TeamRepository teams;
    private final TeamEntryRepository entradas;
    private final PlayerRepository jugadores;
    private final AppUserRepository users;
    private final ArbitroRepository arbitros;
    private final PartidoArbitroRepository partidosArbitros;
    private final TarjetaRepository tarjetas;
    private final TarjetaJugadorRepository tarjetasJugadores;
    private final PartidoSancionRepository partidosSanciones;
    private final PartidoSancionDetalleRepository partidosSancionesDetalle;
    private final SancionJugadorRepository sancionesJugador;
    private final PartidoCobroRepository cobros;
    private final PartidoCobroDetalleRepository detalles;
    private final CanchaRepository canchas;
    private final CierreJornadaService cierres;

    public CobroPartidoService(PartidoRepository partidos, JornadaRepository jornadas, CategoryRepository categories,
                               TeamRepository teams, TeamEntryRepository entradas, PlayerRepository jugadores,
                               AppUserRepository users, ArbitroRepository arbitros, PartidoArbitroRepository partidosArbitros,
                               TarjetaRepository tarjetas, TarjetaJugadorRepository tarjetasJugadores,
                               PartidoSancionRepository partidosSanciones, PartidoSancionDetalleRepository partidosSancionesDetalle,
                               SancionJugadorRepository sancionesJugador, PartidoCobroRepository cobros,
                               PartidoCobroDetalleRepository detalles, CanchaRepository canchas, CierreJornadaService cierres) {
        this.partidos = partidos;
        this.jornadas = jornadas;
        this.categories = categories;
        this.teams = teams;
        this.entradas = entradas;
        this.jugadores = jugadores;
        this.users = users;
        this.arbitros = arbitros;
        this.partidosArbitros = partidosArbitros;
        this.tarjetas = tarjetas;
        this.tarjetasJugadores = tarjetasJugadores;
        this.partidosSanciones = partidosSanciones;
        this.partidosSancionesDetalle = partidosSancionesDetalle;
        this.sancionesJugador = sancionesJugador;
        this.cobros = cobros;
        this.detalles = detalles;
        this.canchas = canchas;
        this.cierres = cierres;
    }

    @Transactional(readOnly = true)
    public CobroViews.CobroView ver(UUID partidoId, UUID equipoId) {
        Partido partido = partidoActivo(partidoId);
        Category categoria = categoria(partido);
        UUID ligaId = categoria.getEdition().getLeague().getId();
        assertRead(ligaId);
        UUID equipo = equipo(partido, equipoId);
        return armar(partido, categoria, ligaId, equipo);
    }

    @Transactional
    public CobroViews.CobroView guardar(UUID partidoId, CobroViews.CobroGuardarRequest request) {
        Partido partido = partidoActivo(partidoId);
        Category categoria = categoria(partido);
        UUID ligaId = categoria.getEdition().getLeague().getId();
        assertWrite(ligaId, partido);
        UUID equipo = equipo(partido, request == null ? null : request.getEquipoId());
        Contexto contexto = contexto(partido, categoria, ligaId, equipo);
        Set<UUID> tarjetasPedidas = ids(request == null ? null : request.getTarjetaIds());
        Set<UUID> sancionesPedidas = ids(request == null ? null : request.getSancionIds());
        for (UUID id : tarjetasPedidas) {
            if (!contexto.tarjetas.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La tarjeta no se puede cobrar en este partido");
            }
        }
        for (UUID id : sancionesPedidas) {
            if (!contexto.sanciones.containsKey(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sanción no se puede cobrar en este partido");
            }
        }
        String usuario = usuario();
        PartidoCobro cobro = contexto.cobro;
        boolean nuevo = cobro == null;
        if (nuevo) {
            cobro = new PartidoCobro();
            cobro.setId(UUID.randomUUID());
            cobro.setIdPartido(partido.getId());
            cobro.setIdEquipo(equipo);
            cobro.setFechaCreacion(OffsetDateTime.now());
            cobro.setUsuarioCreacion(usuario);
        }
        cobro.setIdJornada(partido.getIdJornada());
        cobro.setFecha(contexto.fechaActual);
        cobro.setObservacion(observacion(request == null ? null : request.getObservacion()));
        BigDecimal total = totalDe(contexto, tarjetasPedidas, sancionesPedidas);
        cobro.setValorCobroEfectivo(monto(request == null ? null : request.getValorCobroEfectivo(), total, "El valor cobro efectivo"));
        cobro.setValorCobroTransferencia(monto(request == null ? null : request.getValorCobroTransferencia(),
                BigDecimal.ZERO.setScale(2), "El valor cobro transferencia"));
        cobro.setEstado("ACTIVO");
        cobro.setFechaModificacion(OffsetDateTime.now());
        cobro.setUsuarioModificacion(usuario);
        cobros.save(cobro);
        guardarDetalles(cobro, contexto, tarjetasPedidas, sancionesPedidas, usuario);
        return armar(partido, categoria, ligaId, equipo);
    }

    private void guardarDetalles(PartidoCobro cobro, Contexto contexto, Set<UUID> tarjetasPedidas,
                                 Set<UUID> sancionesPedidas, String usuario) {
        List<PartidoCobroDetalle> existentes = detalles.findByIdPartidoCobro(cobro.getId());
        Set<UUID> conservados = new HashSet<>();
        for (ArbitroCargo arbitro : contexto.arbitros) {
            PartidoCobroDetalle detalle = buscar(existentes, "ARBITRAJE", arbitro.arbitro.getId());
            if (detalle == null) {
                detalle = nuevoDetalle(cobro, "ARBITRAJE", usuario);
                detalle.setIdArbitro(arbitro.arbitro.getId());
            }
            llenar(detalle, cobro, arbitro.arbitro.getNombre(), arbitro.costo, usuario);
            detalles.save(detalle);
            conservados.add(detalle.getId());
        }
        for (UUID id : tarjetasPedidas) {
            CargoTarjeta cargo = contexto.tarjetas.get(id);
            PartidoCobroDetalle detalle = buscar(existentes, "TARJETA", id);
            if (detalle == null) {
                detalle = nuevoDetalle(cobro, "TARJETA", usuario);
                detalle.setIdTarjetaJugador(id);
            }
            llenar(detalle, cobro, descripcion(cargo.jugador, cargo.detalle), cargo.costo, usuario);
            detalles.save(detalle);
            conservados.add(detalle.getId());
        }
        for (UUID id : sancionesPedidas) {
            CargoSancion cargo = contexto.sanciones.get(id);
            PartidoCobroDetalle detalle = buscar(existentes, "SANCION", id);
            if (detalle == null) {
                detalle = nuevoDetalle(cobro, "SANCION", usuario);
                detalle.setIdPartidoSancionDetalle(id);
            }
            llenar(detalle, cobro, descripcion(cargo.jugador, cargo.detalleTexto), cargo.costo, usuario);
            detalles.save(detalle);
            conservados.add(detalle.getId());
        }
        for (PartidoCobroDetalle detalle : existentes) {
            if (conservados.contains(detalle.getId()) || "ELIMINADO".equals(detalle.getEstado())) {
                continue;
            }
            detalle.setEstado("ELIMINADO");
            detalle.setFechaModificacion(OffsetDateTime.now());
            detalle.setUsuarioModificacion(usuario);
            detalles.save(detalle);
        }
    }

    private CobroViews.CobroView armar(Partido partido, Category categoria, UUID ligaId, UUID equipoId) {
        Contexto contexto = contexto(partido, categoria, ligaId, equipoId);
        boolean hayCobro = contexto.cobro != null;
        List<CobroViews.CargoView> tarjetasVista = new ArrayList<>();
        BigDecimal tarjetas = BigDecimal.ZERO.setScale(2);
        for (CargoTarjeta cargo : contexto.tarjetas.values()) {
            boolean marcado = !hayCobro || contexto.tarjetasCobradasAqui.contains(cargo.tarjeta.getId());
            if (marcado) {
                tarjetas = tarjetas.add(cargo.costo);
            }
            tarjetasVista.add(cargo(cargo.tarjeta.getId(), cargo.jugador, cargo.jornada, cargo.detalle, cargo.costo, marcado));
        }
        ordenar(tarjetasVista);
        List<CobroViews.CargoView> sancionesVista = new ArrayList<>();
        BigDecimal sanciones = BigDecimal.ZERO.setScale(2);
        for (CargoSancion cargo : contexto.sanciones.values()) {
            boolean marcado = !hayCobro || contexto.sancionesCobradasAqui.contains(cargo.detalle.getId());
            if (marcado) {
                sanciones = sanciones.add(cargo.costo);
            }
            sancionesVista.add(cargo(cargo.detalle.getId(), cargo.jugador, cargo.jornada, cargo.detalleTexto, cargo.costo, marcado));
        }
        ordenar(sancionesVista);
        BigDecimal arbitraje = BigDecimal.ZERO.setScale(2);
        for (ArbitroCargo arbitro : contexto.arbitros) {
            arbitraje = arbitraje.add(arbitro.costo);
        }
        String observacion = contexto.cobro == null ? null : contexto.cobro.getObservacion();
        BigDecimal total = arbitraje.add(tarjetas).add(sanciones).setScale(2);
        BigDecimal efectivo = contexto.cobro == null || contexto.cobro.getValorCobroEfectivo() == null
                ? total : contexto.cobro.getValorCobroEfectivo().setScale(2);
        BigDecimal transferencia = contexto.cobro == null || contexto.cobro.getValorCobroTransferencia() == null
                ? BigDecimal.ZERO.setScale(2) : contexto.cobro.getValorCobroTransferencia().setScale(2);
        Jornada jornada = jornadas.findById(partido.getIdJornada()).orElse(null);
        return new CobroViews.CobroView(
                nombre(partido.getIdEquipoLocal()),
                nombre(partido.getIdEquipoVisita()),
                opcion(partido.getIdEquipoLocal()),
                opcion(partido.getIdEquipoVisita()),
                jornada == null ? "" : jornada.getNombre(),
                partido.getFecha() != null ? partido.getFecha() : (jornada == null ? null : jornada.getFecha()),
                textoHora(partido.getHora()),
                partido.getIdCancha() == null ? null : canchas.findById(partido.getIdCancha()).map(Cancha::getDescripcion).orElse(null),
                puedeEditar(ligaId, partido),
                equipoId,
                nombre(equipoId),
                arbitraje,
                tarjetasVista,
                sancionesVista,
                observacion,
                total,
                efectivo,
                transferencia);
    }

    private Contexto contexto(Partido partido, Category categoria, UUID ligaId, UUID equipoId) {
        Contexto contexto = new Contexto();
        Map<UUID, Jornada> porJornada = new HashMap<>();
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoria.getId(), "ACTIVO")) {
            porJornada.put(jornada.getId(), jornada);
        }
        jornadas.findById(partido.getIdJornada()).ifPresent(jornada -> porJornada.put(jornada.getId(), jornada));
        Map<UUID, Partido> porPartido = new HashMap<>();
        for (Partido otro : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoria.getId(), "ACTIVO")) {
            porPartido.put(otro.getId(), otro);
        }
        porPartido.put(partido.getId(), partido);
        contexto.fechaActual = fechaDe(partido, porJornada);
        TeamEntry entrada = entradas.findByTeam_IdAndCategory_Id(equipoId, categoria.getId()).orElse(null);
        Map<UUID, Player> porJugador = new HashMap<>();
        if (entrada != null) {
            for (Player jugador : jugadores.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId())) {
                porJugador.put(jugador.getId(), jugador);
            }
        }
        Tarjeta amarilla = cercano(ligaId, true, null);
        Tarjeta roja = cercano(ligaId, false, idDe(amarilla));
        BigDecimal amarillo = costoDe(amarilla);
        BigDecimal rojo = costoDe(roja);
        Set<UUID> tarjetasAjenas = new HashSet<>();
        Set<UUID> sancionesAjenas = new HashSet<>();
        PartidoCobro cobro = null;
        for (PartidoCobro existente : cobros.findByIdPartido(partido.getId())) {
            if (equipoId.equals(existente.getIdEquipo()) && "ACTIVO".equals(existente.getEstado())) {
                cobro = existente;
            }
        }
        contexto.cobro = cobro;
        for (PartidoCobroDetalle detalle : detalles.cargosActivos()) {
            boolean deEste = cobro != null && cobro.getId().equals(detalle.getIdPartidoCobro());
            if ("TARJETA".equals(detalle.getTipo()) && detalle.getIdTarjetaJugador() != null) {
                if (deEste) {
                    contexto.tarjetasCobradasAqui.add(detalle.getIdTarjetaJugador());
                } else {
                    tarjetasAjenas.add(detalle.getIdTarjetaJugador());
                }
            }
            if ("SANCION".equals(detalle.getTipo()) && detalle.getIdPartidoSancionDetalle() != null) {
                if (deEste) {
                    contexto.sancionesCobradasAqui.add(detalle.getIdPartidoSancionDetalle());
                } else {
                    sancionesAjenas.add(detalle.getIdPartidoSancionDetalle());
                }
            }
        }
        for (TarjetaJugador tarjeta : tarjetasJugadores.activasDeCategoria(categoria.getId())) {
            if (tarjetasAjenas.contains(tarjeta.getId()) || partido.getId().equals(tarjeta.getIdPartido())) {
                continue;
            }
            Player jugador = porJugador.get(tarjeta.getIdJugador());
            Partido origen = porPartido.get(tarjeta.getIdPartido());
            if (jugador == null || origen == null || !esPrevia(origen, partido, porJornada)) {
                continue;
            }
            if (tarjeta.getAmarillas() == null && tarjeta.getRojas() == null) {
                continue;
            }
            CargoTarjeta cargo = new CargoTarjeta();
            cargo.tarjeta = tarjeta;
            cargo.jugador = jugador;
            cargo.jornada = nombreJornada(porJornada.get(origen.getIdJornada()));
            cargo.detalle = textoTarjeta(tarjeta);
            cargo.costo = costoTarjeta(tarjeta, amarillo, rojo);
            cargo.orden = ordenJornada(porJornada.get(origen.getIdJornada()));
            contexto.tarjetas.put(tarjeta.getId(), cargo);
        }
        for (PartidoSancion sancion : partidosSanciones.activasDeCategoria(categoria.getId())) {
            if (partido.getId().equals(sancion.getIdPartido())) {
                continue;
            }
            Player jugador = porJugador.get(sancion.getIdJugador());
            Partido origen = porPartido.get(sancion.getIdPartido());
            if (jugador == null || origen == null || !esPrevia(origen, partido, porJornada)) {
                continue;
            }
            for (PartidoSancionDetalle detalle : partidosSancionesDetalle.findByIdPartidoSancion(sancion.getId())) {
                if (!"ACTIVO".equals(detalle.getEstado()) || sancionesAjenas.contains(detalle.getId())) {
                    continue;
                }
                SancionJugador catalogo = sancionesJugador.findById(detalle.getIdSancionJugador()).orElse(null);
                CargoSancion cargo = new CargoSancion();
                cargo.detalle = detalle;
                cargo.jugador = jugador;
                cargo.jornada = nombreJornada(porJornada.get(origen.getIdJornada()));
                cargo.detalleTexto = catalogo == null ? "Sanción" : catalogo.getDescripcion();
                cargo.costo = catalogo == null || catalogo.getCosto() == null
                        ? BigDecimal.ZERO.setScale(2) : catalogo.getCosto().setScale(2);
                cargo.orden = ordenJornada(porJornada.get(origen.getIdJornada()));
                contexto.sanciones.put(detalle.getId(), cargo);
            }
        }
        for (PartidoArbitro asignado : partidosArbitros.findByIdPartido(partido.getId())) {
            if (!"ACTIVO".equals(asignado.getEstado())) {
                continue;
            }
            Arbitro arbitro = arbitros.findById(asignado.getIdArbitro()).orElse(null);
            if (arbitro == null || !"ACTIVO".equals(arbitro.getEstado())) {
                continue;
            }
            ArbitroCargo cargo = new ArbitroCargo();
            cargo.arbitro = arbitro;
            cargo.costo = arbitro.getValorCobroEquipo() == null
                    ? BigDecimal.ZERO.setScale(2) : arbitro.getValorCobroEquipo().setScale(2);
            contexto.arbitros.add(cargo);
        }
        return contexto;
    }

    private Tarjeta cercano(UUID ligaId, boolean amarillo, UUID excepto) {
        Tarjeta mejor = null;
        int distancia = Integer.MAX_VALUE;
        int objetivoR = amarillo ? AMARILLO_R : ROJO_R;
        int objetivoG = amarillo ? AMARILLO_G : ROJO_G;
        int objetivoB = amarillo ? AMARILLO_B : ROJO_B;
        for (Tarjeta tarjeta : tarjetas.buscar(ligaId, "", "ACTIVO")) {
            if (excepto != null && excepto.equals(tarjeta.getId())) {
                continue;
            }
            int actual = distancia(tarjeta.getColor(), objetivoR, objetivoG, objetivoB);
            if (actual < distancia) {
                distancia = actual;
                mejor = tarjeta;
            }
        }
        return distancia == Integer.MAX_VALUE ? null : mejor;
    }

    private UUID idDe(Tarjeta tarjeta) {
        return tarjeta == null ? null : tarjeta.getId();
    }

    private BigDecimal costoDe(Tarjeta tarjeta) {
        if (tarjeta == null || tarjeta.getCosto() == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return tarjeta.getCosto().setScale(2);
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

    private BigDecimal costoTarjeta(TarjetaJugador tarjeta, BigDecimal amarillo, BigDecimal rojo) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        if (tarjeta.getAmarillas() != null) {
            total = total.add(amarillo.multiply(BigDecimal.valueOf(tarjeta.getAmarillas())));
        }
        if (tarjeta.getRojas() != null) {
            total = total.add(rojo.multiply(BigDecimal.valueOf(tarjeta.getRojas())));
        }
        return total.setScale(2);
    }

    private boolean esPrevia(Partido origen, Partido actual, Map<UUID, Jornada> porJornada) {
        Jornada jornadaOrigen = porJornada.get(origen.getIdJornada());
        Jornada jornadaActual = porJornada.get(actual.getIdJornada());
        if (jornadaOrigen == null || jornadaActual == null) {
            LocalDate fecha = fechaDe(origen, porJornada);
            LocalDate hoy = fechaDe(actual, porJornada);
            return hoy != null && fecha != null && fecha.isBefore(hoy);
        }
        return jornadaOrigen.getNumero() < jornadaActual.getNumero();
    }

    private String textoHora(java.time.LocalTime hora) {
        if (hora == null) {
            return null;
        }
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }

    private LocalDate fechaDe(Partido partido, Map<UUID, Jornada> porJornada) {
        if (partido.getFecha() != null) {
            return partido.getFecha();
        }
        Jornada jornada = porJornada.get(partido.getIdJornada());
        return jornada == null ? null : jornada.getFecha();
    }

    private String textoTarjeta(TarjetaJugador tarjeta) {
        List<String> partes = new ArrayList<>();
        if (tarjeta.getAmarillas() != null) {
            partes.add(tarjeta.getAmarillas() + (tarjeta.getAmarillas() == 1 ? " amarilla" : " amarillas"));
        }
        if (tarjeta.getRojas() != null) {
            partes.add("1 roja");
        }
        return String.join(", ", partes);
    }

    private CobroViews.CargoView cargo(UUID id, Player jugador, String jornada, String detalle, BigDecimal costo, boolean marcado) {
        String alias = jugador.getAlias() == null || jugador.getAlias().trim().isEmpty() ? jugador.getName() : jugador.getAlias();
        return new CobroViews.CargoView(
                id, jugador.getId(), jugador.getNumeroCamisa(), alias, jugador.getPhoto() != null,
                jugador.getRotationDegrees(), jornada, detalle, costo, marcado);
    }

    private void ordenar(List<CobroViews.CargoView> cargos) {
        cargos.sort(Comparator.comparing(CobroViews.CargoView::getJornada, Comparator.nullsLast(String::compareTo))
                .thenComparingInt(CobroViews.CargoView::getNumero)
                .thenComparing(CobroViews.CargoView::getAlias));
    }

    private PartidoCobroDetalle buscar(List<PartidoCobroDetalle> existentes, String tipo, UUID origen) {
        for (PartidoCobroDetalle detalle : existentes) {
            if (!tipo.equals(detalle.getTipo())) {
                continue;
            }
            UUID actual = "ARBITRAJE".equals(tipo) ? detalle.getIdArbitro()
                    : "TARJETA".equals(tipo) ? detalle.getIdTarjetaJugador() : detalle.getIdPartidoSancionDetalle();
            if (origen.equals(actual)) {
                return detalle;
            }
        }
        return null;
    }

    private PartidoCobroDetalle nuevoDetalle(PartidoCobro cobro, String tipo, String usuario) {
        PartidoCobroDetalle detalle = new PartidoCobroDetalle();
        detalle.setId(UUID.randomUUID());
        detalle.setIdPartidoCobro(cobro.getId());
        detalle.setTipo(tipo);
        detalle.setFechaCreacion(OffsetDateTime.now());
        detalle.setUsuarioCreacion(usuario);
        return detalle;
    }

    private void llenar(PartidoCobroDetalle detalle, PartidoCobro cobro, String descripcion, BigDecimal costo, String usuario) {
        detalle.setIdPartido(cobro.getIdPartido());
        detalle.setIdJornada(cobro.getIdJornada());
        detalle.setFecha(cobro.getFecha());
        detalle.setIdEquipo(cobro.getIdEquipo());
        detalle.setDescripcion(limitar(descripcion, 200));
        detalle.setCosto(costo == null ? BigDecimal.ZERO.setScale(2) : costo.setScale(2));
        detalle.setEstado("ACTIVO");
        detalle.setFechaModificacion(OffsetDateTime.now());
        detalle.setUsuarioModificacion(usuario);
    }

    private String descripcion(Player jugador, String detalle) {
        String alias = jugador.getAlias() == null || jugador.getAlias().trim().isEmpty() ? jugador.getName() : jugador.getAlias();
        return alias + " " + jugador.getNumeroCamisa() + " " + detalle;
    }

    private String limitar(String texto, int maximo) {
        String valor = texto == null ? "" : texto.trim();
        if (valor.isEmpty()) {
            return "Cargo";
        }
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private BigDecimal totalDe(Contexto contexto, Set<UUID> tarjetasPedidas, Set<UUID> sancionesPedidas) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (ArbitroCargo arbitro : contexto.arbitros) {
            total = total.add(arbitro.costo);
        }
        for (UUID id : tarjetasPedidas) {
            total = total.add(contexto.tarjetas.get(id).costo);
        }
        for (UUID id : sancionesPedidas) {
            total = total.add(contexto.sanciones.get(id).costo);
        }
        return total.setScale(2);
    }

    private BigDecimal monto(BigDecimal valor, BigDecimal siNulo, String etiqueta) {
        if (valor == null) {
            return siNulo.setScale(2);
        }
        if (valor.signum() < 0 || valor.scale() > 2 && valor.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, etiqueta + " debe ser cero o mayor, con hasta dos decimales");
        }
        return valor.setScale(2);
    }

    private String observacion(String valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.length() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La observación admite hasta 300 caracteres");
        }
        return texto;
    }

    private Set<UUID> ids(List<UUID> valores) {
        Set<UUID> ids = new HashSet<>();
        if (valores == null) {
            return ids;
        }
        for (UUID id : valores) {
            if (id != null && !ids.add(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cargo está repetido");
            }
        }
        return ids;
    }

    private UUID equipo(Partido partido, UUID equipoId) {
        if (equipoId == null) {
            if (partido.getIdEquipoLocal() != null) {
                return partido.getIdEquipoLocal();
            }
            if (partido.getIdEquipoVisita() != null) {
                return partido.getIdEquipoVisita();
            }
        }
        if (equipoId != null && (equipoId.equals(partido.getIdEquipoLocal()) || equipoId.equals(partido.getIdEquipoVisita()))) {
            return equipoId;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no juega este partido");
    }

    private String nombreJornada(Jornada jornada) {
        return jornada == null ? "" : jornada.getNombre();
    }

    private int ordenJornada(Jornada jornada) {
        return jornada == null ? Integer.MAX_VALUE : jornada.getNumero();
    }

    private CobroViews.EquipoView opcion(UUID id) {
        if (id == null) {
            return null;
        }
        return new CobroViews.EquipoView(id, nombre(id));
    }

    private String nombre(UUID id) {
        if (id == null) {
            return "Por definir";
        }
        return teams.findById(id).map(Team::getName).orElse("Por definir");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe"));
    }

    private boolean puedeEditar(UUID leagueId, Partido partido) {
        if (cierres.jornadaCerrada(partido.getIdJornada())) {
            return false;
        }
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

    private void assertWrite(UUID leagueId, Partido partido) {
        assertRead(leagueId);
        if (cierres.jornadaCerrada(partido.getIdJornada())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La jornada está cerrada");
        }
        if (!puedeEditar(leagueId, partido)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este marcador");
        }
    }

    private void assertRead(UUID leagueId) {
        AppUser user = actual();
        boolean miembro = user.isMaster() || user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId));
        if (!miembro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta liga");
        }
    }

    private String usuario() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private AppUser actual() {
        return users.findWithMembershipsByUsername(usuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private static final class Contexto {
        private LocalDate fechaActual;
        private PartidoCobro cobro;
        private final List<ArbitroCargo> arbitros = new ArrayList<>();
        private final Map<UUID, CargoTarjeta> tarjetas = new HashMap<>();
        private final Map<UUID, CargoSancion> sanciones = new HashMap<>();
        private final Set<UUID> tarjetasCobradasAqui = new HashSet<>();
        private final Set<UUID> sancionesCobradasAqui = new HashSet<>();
    }

    private static final class ArbitroCargo {
        private Arbitro arbitro;
        private BigDecimal costo;
    }

    private static final class CargoTarjeta {
        private TarjetaJugador tarjeta;
        private Player jugador;
        private String jornada;
        private String detalle;
        private BigDecimal costo;
        private int orden;
    }

    private static final class CargoSancion {
        private PartidoSancionDetalle detalle;
        private Player jugador;
        private String jornada;
        private String detalleTexto;
        private BigDecimal costo;
        private int orden;
    }
}
