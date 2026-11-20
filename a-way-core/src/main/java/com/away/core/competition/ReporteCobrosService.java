package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReporteCobrosService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static final int AMARILLO_R = 0xF5;
    private static final int AMARILLO_G = 0xC5;
    private static final int AMARILLO_B = 0x18;
    private static final int ROJO_R = 0xC6;
    private static final int ROJO_G = 0x28;
    private static final int ROJO_B = 0x28;
    private static final int FILAS_EN_HOJA = 24;

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final TeamRepository teams;
    private final TarjetaRepository catalogo;
    private final TarjetaJugadorRepository tarjetasJugadores;
    private final PartidoCobroRepository cobros;
    private final PartidoCobroDetalleRepository detalles;
    private final AppUserRepository users;

    public ReporteCobrosService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                                PartidoRepository partidos, TeamRepository teams, TarjetaRepository catalogo,
                                TarjetaJugadorRepository tarjetasJugadores, PartidoCobroRepository cobros,
                                PartidoCobroDetalleRepository detalles, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.teams = teams;
        this.catalogo = catalogo;
        this.tarjetasJugadores = tarjetasJugadores;
        this.cobros = cobros;
        this.detalles = detalles;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public byte[] cobros(UUID editionId, UUID categoryId, LocalDate fecha) {
        if (fecha == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una fecha");
        }
        Edition edition = editions.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
        League liga = edition.getLeague();
        assertRead(liga.getId());
        List<Category> elegidas = categorias(edition, categoryId);
        List<Fila> filas = filas(liga.getId(), elegidas, fecha);
        String subtitulo = fecha.format(DIA);
        if (categoryId != null && elegidas.size() == 1) {
            subtitulo = subtitulo + " · " + elegidas.get(0).getName();
        }
        String cuerpo = filas.isEmpty()
                ? "<p class=\"vacio\">No hay partidos en esta fecha.</p>"
                : tablas(filas);
        return ReportePdf.deHtml(documento(liga, subtitulo, cuerpo));
    }

    private List<Category> categorias(Edition edition, UUID categoryId) {
        List<Category> elegidas = new ArrayList<>();
        boolean encontrada = categoryId == null;
        for (Category categoria : categories.findByEdition_IdAndEstadoOrderByNameAsc(edition.getId(), "ACTIVO")) {
            if (categoryId == null || categoryId.equals(categoria.getId())) {
                elegidas.add(categoria);
                encontrada = true;
            }
        }
        if (!encontrada) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a esta edición");
        }
        return elegidas;
    }

    private List<Fila> filas(UUID ligaId, List<Category> elegidas, LocalDate fecha) {
        List<UUID> categorias = new ArrayList<>();
        Map<UUID, String> nombreCategoria = new HashMap<>();
        for (Category categoria : elegidas) {
            categorias.add(categoria.getId());
            nombreCategoria.put(categoria.getId(), categoria.getName());
        }
        if (categorias.isEmpty()) {
            return new ArrayList<>();
        }
        Map<UUID, Jornada> porJornada = new HashMap<>();
        for (Jornada jornada : jornadas.findByIdCategoriaInAndEstado(categorias, "ACTIVO")) {
            porJornada.put(jornada.getId(), jornada);
        }
        List<Partido> delDia = new ArrayList<>();
        for (Partido partido : partidos.findByIdCategoriaInAndEstado(categorias, "ACTIVO")) {
            Jornada jornada = porJornada.get(partido.getIdJornada());
            if (jornada == null) {
                continue;
            }
            LocalDate dia = partido.getFecha() != null ? partido.getFecha() : jornada.getFecha();
            if (fecha.equals(dia)) {
                delDia.add(partido);
            }
        }
        Map<UUID, String> nombreEquipo = nombresEquipo(delDia);
        delDia.sort(Comparator
                .comparing((Partido partido) -> textoHora(partido.getHora()), Comparator.nullsLast(String::compareTo))
                .thenComparing(partido -> nombreCategoria.getOrDefault(partido.getIdCategoria(), ""))
                .thenComparing(partido -> nombreJornada(porJornada.get(partido.getIdJornada())))
                .thenComparingInt(Partido::getOrden));
        Map<String, Montos> montos = montos(ligaId, delDia);
        List<Fila> filas = new ArrayList<>();
        for (Partido partido : delDia) {
            String hora = textoHora(partido.getHora());
            String categoria = nombreCategoria.getOrDefault(partido.getIdCategoria(), "");
            String jornada = nombreJornada(porJornada.get(partido.getIdJornada()));
            String local = nombreEquipo.getOrDefault(partido.getIdEquipoLocal(), "Por definir");
            String visita = nombreEquipo.getOrDefault(partido.getIdEquipoVisita(), "Por definir");
            String cruce = local + " VS " + visita;
            filas.add(fila(hora, categoria, jornada, cruce, sumar(
                    montos.get(llave(partido.getId(), partido.getIdEquipoLocal())),
                    montos.get(llave(partido.getId(), partido.getIdEquipoVisita())))));
        }
        return filas;
    }

    private Map<String, Montos> montos(UUID ligaId, List<Partido> delDia) {
        Map<String, Montos> porEquipo = new HashMap<>();
        if (delDia.isEmpty()) {
            return porEquipo;
        }
        List<UUID> ids = new ArrayList<>();
        for (Partido partido : delDia) {
            ids.add(partido.getId());
        }
        Set<UUID> tarjetas = new HashSet<>();
        List<PartidoCobroDetalle> lineas = detalles.activosDePartidos(ids);
        for (PartidoCobroDetalle detalle : lineas) {
            if ("TARJETA".equals(detalle.getTipo()) && detalle.getIdTarjetaJugador() != null) {
                tarjetas.add(detalle.getIdTarjetaJugador());
            }
        }
        Map<UUID, TarjetaJugador> porTarjeta = new HashMap<>();
        if (!tarjetas.isEmpty()) {
            for (TarjetaJugador tarjeta : tarjetasJugadores.findAllById(tarjetas)) {
                porTarjeta.put(tarjeta.getId(), tarjeta);
            }
        }
        BigDecimal costoAmarillo = costoDe(cercano(ligaId, true, null));
        BigDecimal costoRojo = costoDe(cercano(ligaId, false, idDe(cercano(ligaId, true, null))));
        for (PartidoCobroDetalle detalle : lineas) {
            Montos montos = porEquipo.computeIfAbsent(llave(detalle.getIdPartido(), detalle.getIdEquipo()), clave -> new Montos());
            BigDecimal costo = monto(detalle.getCosto());
            if ("ARBITRAJE".equals(detalle.getTipo())) {
                montos.arbitraje = montos.arbitraje.add(costo);
            } else if ("TARJETA".equals(detalle.getTipo())) {
                sumarTarjeta(montos, detalle, porTarjeta.get(detalle.getIdTarjetaJugador()), costoAmarillo, costoRojo);
            } else if ("SANCION".equals(detalle.getTipo())) {
                montos.suspensiones = montos.suspensiones.add(costo);
            }
        }
        for (PartidoCobro cobro : cobros.activosDePartidos(ids)) {
            Montos montos = porEquipo.computeIfAbsent(llave(cobro.getIdPartido(), cobro.getIdEquipo()), clave -> new Montos());
            montos.efectivo = montos.efectivo.add(monto(cobro.getValorCobroEfectivo()));
            montos.transferencia = montos.transferencia.add(monto(cobro.getValorCobroTransferencia()));
        }
        return porEquipo;
    }

    private void sumarTarjeta(Montos montos, PartidoCobroDetalle detalle, TarjetaJugador tarjeta,
                              BigDecimal costoAmarillo, BigDecimal costoRojo) {
        BigDecimal costo = monto(detalle.getCosto());
        int amarillas = tarjeta == null || tarjeta.getAmarillas() == null ? 0 : tarjeta.getAmarillas();
        int rojas = tarjeta == null || tarjeta.getRojas() == null ? 0 : tarjeta.getRojas();
        if (amarillas > 0 && rojas == 0) {
            montos.amarillas = montos.amarillas.add(costo);
            return;
        }
        if (rojas > 0 && amarillas == 0) {
            montos.rojas = montos.rojas.add(costo);
            return;
        }
        if (amarillas > 0 && rojas > 0) {
            BigDecimal pesoAmarillo = costoAmarillo.multiply(BigDecimal.valueOf(amarillas));
            BigDecimal pesoRojo = costoRojo.multiply(BigDecimal.valueOf(rojas));
            BigDecimal pesos = pesoAmarillo.add(pesoRojo);
            if (pesos.signum() == 0) {
                pesoAmarillo = BigDecimal.valueOf(amarillas);
                pesoRojo = BigDecimal.valueOf(rojas);
                pesos = pesoAmarillo.add(pesoRojo);
            }
            BigDecimal parteAmarilla = costo.multiply(pesoAmarillo).divide(pesos, 2, RoundingMode.HALF_UP);
            montos.amarillas = montos.amarillas.add(parteAmarilla);
            montos.rojas = montos.rojas.add(costo.subtract(parteAmarilla));
            return;
        }
        String descripcion = detalle.getDescripcion() == null ? "" : detalle.getDescripcion().toUpperCase(Locale.ROOT);
        if (descripcion.contains("ROJA") && !descripcion.contains("AMARILL")) {
            montos.rojas = montos.rojas.add(costo);
        } else {
            montos.amarillas = montos.amarillas.add(costo);
        }
    }

    private Map<UUID, String> nombresEquipo(List<Partido> delDia) {
        Set<UUID> ids = new HashSet<>();
        for (Partido partido : delDia) {
            if (partido.getIdEquipoLocal() != null) {
                ids.add(partido.getIdEquipoLocal());
            }
            if (partido.getIdEquipoVisita() != null) {
                ids.add(partido.getIdEquipoVisita());
            }
        }
        Map<UUID, String> nombres = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Team equipo : teams.findAllById(ids)) {
                nombres.put(equipo.getId(), equipo.getName());
            }
        }
        return nombres;
    }

    private String nombreJornada(Jornada jornada) {
        if (jornada == null || jornada.getNombre() == null) {
            return "";
        }
        return jornada.getNombre();
    }

    private String tablas(List<Fila> filas) {
        String suspensiones = haySuspensiones(filas) ? tablaSuspensiones(filas) : "";
        return tablaArbitraje(filas) + tablaTarjetas(filas) + suspensiones + tablaResumen(filas);
    }

    private boolean haySuspensiones(List<Fila> filas) {
        for (Fila fila : filas) {
            if (fila.suspensiones.signum() > 0) {
                return true;
            }
        }
        return false;
    }

    private String tablaArbitraje(List<Fila> filas) {
        StringBuilder cuerpo = new StringBuilder();
        BigDecimal total = cero();
        for (Fila fila : filas) {
            cuerpo.append("<tr>");
            identidad(cuerpo, fila);
            celda(cuerpo, fila.arbitraje);
            cuerpo.append("</tr>");
            total = total.add(fila.arbitraje);
        }
        cuerpo.append("<tr class=\"total\"><td colspan=\"4\">Total</td>");
        celda(cuerpo, total);
        cuerpo.append("</tr>");
        return tabla("COBRO DE ARBITRAJE",
                "<th>Hora</th><th>Categoría</th><th>Jornada</th><th>Partido</th><th class=\"monto\">Arbitraje</th>",
                cuerpo.toString(), filas.size());
    }

    private String tablaTarjetas(List<Fila> filas) {
        StringBuilder cuerpo = new StringBuilder();
        BigDecimal amarillas = cero();
        BigDecimal rojas = cero();
        for (Fila fila : filas) {
            BigDecimal total = fila.amarillas.add(fila.rojas);
            cuerpo.append("<tr>");
            identidad(cuerpo, fila);
            celda(cuerpo, fila.amarillas);
            celda(cuerpo, fila.rojas);
            celda(cuerpo, total);
            cuerpo.append("</tr>");
            amarillas = amarillas.add(fila.amarillas);
            rojas = rojas.add(fila.rojas);
        }
        cuerpo.append("<tr class=\"total\"><td colspan=\"4\">Total</td>");
        celda(cuerpo, amarillas);
        celda(cuerpo, rojas);
        celda(cuerpo, amarillas.add(rojas));
        cuerpo.append("</tr>");
        return tabla("COBRO DE TARJETAS",
                "<th>Hora</th><th>Categoría</th><th>Jornada</th><th>Partido</th>"
                        + "<th class=\"monto\">Amarillas</th><th class=\"monto\">Rojas</th><th class=\"monto\">Total</th>",
                cuerpo.toString(), filas.size());
    }

    private String tablaSuspensiones(List<Fila> filas) {
        StringBuilder cuerpo = new StringBuilder();
        BigDecimal total = cero();
        for (Fila fila : filas) {
            cuerpo.append("<tr>");
            identidad(cuerpo, fila);
            celda(cuerpo, fila.suspensiones);
            cuerpo.append("</tr>");
            total = total.add(fila.suspensiones);
        }
        cuerpo.append("<tr class=\"total\"><td colspan=\"4\">Total</td>");
        celda(cuerpo, total);
        cuerpo.append("</tr>");
        return tabla("COBRO DE SUSPENSIONES",
                "<th>Hora</th><th>Categoría</th><th>Jornada</th><th>Partido</th><th class=\"monto\">Suspensiones</th>",
                cuerpo.toString(), filas.size());
    }

    private String tablaResumen(List<Fila> filas) {
        StringBuilder cuerpo = new StringBuilder();
        BigDecimal arbitraje = cero();
        BigDecimal tarjetas = cero();
        BigDecimal suspensiones = cero();
        BigDecimal efectivo = cero();
        BigDecimal transferencia = cero();
        for (Fila fila : filas) {
            BigDecimal tarjetasFila = fila.amarillas.add(fila.rojas);
            BigDecimal total = fila.arbitraje.add(tarjetasFila).add(fila.suspensiones);
            cuerpo.append("<tr>");
            identidad(cuerpo, fila);
            celda(cuerpo, fila.arbitraje);
            celda(cuerpo, tarjetasFila);
            celda(cuerpo, fila.suspensiones);
            celda(cuerpo, total);
            celda(cuerpo, fila.efectivo);
            celda(cuerpo, fila.transferencia);
            cuerpo.append("</tr>");
            arbitraje = arbitraje.add(fila.arbitraje);
            tarjetas = tarjetas.add(tarjetasFila);
            suspensiones = suspensiones.add(fila.suspensiones);
            efectivo = efectivo.add(fila.efectivo);
            transferencia = transferencia.add(fila.transferencia);
        }
        cuerpo.append("<tr class=\"total\"><td colspan=\"4\">Total</td>");
        celda(cuerpo, arbitraje);
        celda(cuerpo, tarjetas);
        celda(cuerpo, suspensiones);
        celda(cuerpo, arbitraje.add(tarjetas).add(suspensiones));
        celda(cuerpo, efectivo);
        celda(cuerpo, transferencia);
        cuerpo.append("</tr>");
        return tabla("RESUMEN",
                "<th>Hora</th><th>Categoría</th><th>Jornada</th><th>Partido</th>"
                        + "<th class=\"monto\">Arbitraje</th><th class=\"monto\">Tarjetas</th><th class=\"monto\">Suspensiones</th>"
                        + "<th class=\"monto\">Total</th><th class=\"monto\">Cobro real</th><th class=\"monto\">Transferencia</th>",
                cuerpo.toString(), filas.size());
    }

    private String tabla(String titulo, String columnas, String cuerpo, int filas) {
        String clase = filas + 1 > FILAS_EN_HOJA ? "bloque salta" : "bloque";
        return "<div class=\"" + clase + "\"><h2>" + esc(titulo) + "</h2><table><thead><tr>" + columnas
                + "</tr></thead><tbody>" + cuerpo + "</tbody></table></div>";
    }

    private void identidad(StringBuilder html, Fila fila) {
        html.append("<td>").append(esc(fila.hora == null ? "Sin hora" : fila.hora)).append("</td>");
        html.append("<td>").append(esc(fila.categoria)).append("</td>");
        html.append("<td>").append(esc(fila.jornada)).append("</td>");
        html.append("<td>").append(esc(fila.partido)).append("</td>");
    }

    private void celda(StringBuilder html, BigDecimal valor) {
        html.append("<td class=\"monto\">").append(dinero(valor)).append("</td>");
    }

    private Fila fila(String hora, String categoria, String jornada, String partido, Montos montos) {
        Montos origen = montos == null ? new Montos() : montos;
        return new Fila(hora, categoria, jornada, partido, origen.arbitraje, origen.amarillas, origen.rojas,
                origen.suspensiones, origen.efectivo, origen.transferencia);
    }

    private Montos sumar(Montos izquierda, Montos derecha) {
        Montos total = new Montos();
        acumular(total, izquierda);
        acumular(total, derecha);
        return total;
    }

    private void acumular(Montos destino, Montos origen) {
        if (origen == null) {
            return;
        }
        destino.arbitraje = destino.arbitraje.add(origen.arbitraje);
        destino.amarillas = destino.amarillas.add(origen.amarillas);
        destino.rojas = destino.rojas.add(origen.rojas);
        destino.suspensiones = destino.suspensiones.add(origen.suspensiones);
        destino.efectivo = destino.efectivo.add(origen.efectivo);
        destino.transferencia = destino.transferencia.add(origen.transferencia);
    }

    private String llave(UUID partido, UUID equipo) {
        return partido + ":" + equipo;
    }

    private Tarjeta cercano(UUID ligaId, boolean amarillo, UUID excepto) {
        Tarjeta mejor = null;
        int distancia = Integer.MAX_VALUE;
        int objetivoR = amarillo ? AMARILLO_R : ROJO_R;
        int objetivoG = amarillo ? AMARILLO_G : ROJO_G;
        int objetivoB = amarillo ? AMARILLO_B : ROJO_B;
        for (Tarjeta tarjeta : catalogo.buscar(ligaId, "", "ACTIVO")) {
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
            return cero();
        }
        return tarjeta.getCosto().setScale(2, RoundingMode.HALF_UP);
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

    private String documento(League liga, String subtitulo, String cuerpo) {
        String logo = "";
        if (liga.getLogo() != null && liga.getLogo().length > 0) {
            String tipo = liga.getLogoContentType() == null ? "image/png" : liga.getLogoContentType();
            logo = "<img class=\"logo\" src=\"data:" + tipo + ";base64,"
                    + Base64.getEncoder().encodeToString(liga.getLogo()) + "\" alt=\"\"/>";
        }
        return "<html><head><meta charset=\"utf-8\"/><style>"
                + "@page { size: 297mm 210mm; margin: 10mm 8mm 10mm; @bottom-center { content: counter(page); font-size: 9px; } }"
                + "body { font-family: sans-serif; font-size: 9px; color: #163a5f; }"
                + ".cabeza { text-align: center; margin-bottom: 12px; }"
                + ".logo { height: 58px; }"
                + "h1 { font-size: 16px; margin: 6px 0 0; }"
                + "h2 { font-size: 13px; margin: 14px 0 6px; page-break-after: avoid; }"
                + ".bloque { page-break-inside: avoid; }"
                + ".bloque.salta { page-break-before: always; page-break-inside: auto; }"
                + "p.sub { margin: 2px 0 0; font-weight: 700; }"
                + "p.generado { margin: 2px 0 0; font-size: 9px; }"
                + "p.vacio { font-weight: 700; }"
                + "table { width: 100%; border-collapse: collapse; margin: 0 0 8px; page-break-inside: avoid; }"
                + ".bloque.salta table { page-break-inside: auto; }"
                + "thead { display: table-header-group; }"
                + "tr { page-break-inside: avoid; }"
                + "th, td { border: 1px solid #163a5f; padding: 3px 4px; vertical-align: top; }"
                + "th { background: #163a5f; color: white; }"
                + "td.monto, th.monto { text-align: right; white-space: nowrap; }"
                + "tr.total td { font-weight: 700; background: #163a5f; color: white; }"
                + "</style></head><body><div class=\"cabeza\">" + logo
                + "<h1>" + esc(liga.getName()) + "</h1>"
                + "<p class=\"sub\">COBROS</p>"
                + "<p class=\"sub\">" + esc(subtitulo) + "</p>"
                + "<p class=\"generado\">" + ZonedDateTime.now(ZONA).format(GENERADO) + "</p></div>"
                + cuerpo + "</body></html>";
    }

    private String textoHora(LocalTime hora) {
        if (hora == null) {
            return null;
        }
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }

    private BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private BigDecimal monto(BigDecimal valor) {
        if (valor == null) {
            return cero();
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String dinero(BigDecimal valor) {
        return monto(valor).toPlainString();
    }

    private String esc(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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

    private static final class Montos {
        private BigDecimal arbitraje = BigDecimal.ZERO.setScale(2);
        private BigDecimal amarillas = BigDecimal.ZERO.setScale(2);
        private BigDecimal rojas = BigDecimal.ZERO.setScale(2);
        private BigDecimal suspensiones = BigDecimal.ZERO.setScale(2);
        private BigDecimal efectivo = BigDecimal.ZERO.setScale(2);
        private BigDecimal transferencia = BigDecimal.ZERO.setScale(2);
    }

    private static final class Fila {
        private final String hora;
        private final String categoria;
        private final String jornada;
        private final String partido;
        private final BigDecimal arbitraje;
        private final BigDecimal amarillas;
        private final BigDecimal rojas;
        private final BigDecimal suspensiones;
        private final BigDecimal efectivo;
        private final BigDecimal transferencia;

        private Fila(String hora, String categoria, String jornada, String partido,
                     BigDecimal arbitraje, BigDecimal amarillas, BigDecimal rojas, BigDecimal suspensiones,
                     BigDecimal efectivo, BigDecimal transferencia) {
            this.hora = hora;
            this.categoria = categoria;
            this.jornada = jornada;
            this.partido = partido;
            this.arbitraje = arbitraje;
            this.amarillas = amarillas;
            this.rojas = rojas;
            this.suspensiones = suspensiones;
            this.efectivo = efectivo;
            this.transferencia = transferencia;
        }
    }
}
