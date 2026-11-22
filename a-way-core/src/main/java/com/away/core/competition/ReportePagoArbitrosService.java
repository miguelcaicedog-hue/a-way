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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReportePagoArbitrosService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static final int FILAS_EN_HOJA = 24;

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final TeamRepository teams;
    private final PartidoArbitroRepository asignaciones;
    private final ArbitroRepository arbitros;
    private final AppUserRepository users;

    public ReportePagoArbitrosService(EditionRepository editions, CategoryRepository categories,
                                      JornadaRepository jornadas, PartidoRepository partidos, TeamRepository teams,
                                      PartidoArbitroRepository asignaciones, ArbitroRepository arbitros,
                                      AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.teams = teams;
        this.asignaciones = asignaciones;
        this.arbitros = arbitros;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public byte[] pago(UUID editionId, UUID categoryId, LocalDate fecha) {
        if (fecha == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una fecha");
        }
        Edition edition = editions.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
        League liga = edition.getLeague();
        assertRead(liga.getId());
        List<Category> elegidas = categorias(edition, categoryId);
        List<Grupo> grupos = grupos(elegidas, fecha);
        String subtitulo = fecha.format(DIA);
        if (categoryId != null && elegidas.size() == 1) {
            subtitulo = subtitulo + " · " + elegidas.get(0).getName();
        }
        String cuerpo = grupos.isEmpty()
                ? "<p class=\"vacio\">No hay árbitros asignados.</p>"
                : tablas(grupos);
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

    private List<Grupo> grupos(List<Category> elegidas, LocalDate fecha) {
        Map<UUID, Vista> vistas = partidos(elegidas, fecha);
        List<Grupo> grupos = new ArrayList<>();
        if (vistas.isEmpty()) {
            return grupos;
        }
        Map<UUID, List<Linea>> porArbitro = new HashMap<>();
        for (PartidoArbitro asignado : asignaciones.activosDePartidos(vistas.keySet())) {
            Vista vista = vistas.get(asignado.getIdPartido());
            if (vista == null) {
                continue;
            }
            porArbitro.computeIfAbsent(asignado.getIdArbitro(), id -> new ArrayList<>())
                    .add(linea(vista, asignado.isPito()));
        }
        if (porArbitro.isEmpty()) {
            return grupos;
        }
        Map<UUID, Arbitro> ficha = new HashMap<>();
        for (Arbitro arbitro : arbitros.findAllById(porArbitro.keySet())) {
            ficha.put(arbitro.getId(), arbitro);
        }
        List<UUID> orden = new ArrayList<>(porArbitro.keySet());
        orden.sort(Comparator.comparing(id -> nombre(ficha.get(id)), String.CASE_INSENSITIVE_ORDER));
        for (UUID id : orden) {
            Arbitro arbitro = ficha.get(id);
            List<Linea> lineas = porArbitro.get(id);
            for (Linea linea : lineas) {
                linea.pago = pago(linea);
            }
            lineas.sort(Comparator
                    .comparing((Linea linea) -> linea.hora == null ? "" : linea.hora)
                    .thenComparing(linea -> linea.categoria)
                    .thenComparing(linea -> linea.jornada)
                    .thenComparing(linea -> linea.partido));
            grupos.add(new Grupo(nombre(arbitro), lineas));
        }
        return grupos;
    }

    private Map<UUID, Vista> partidos(List<Category> elegidas, LocalDate fecha) {
        Map<UUID, Vista> vistas = new HashMap<>();
        List<UUID> categorias = new ArrayList<>();
        Map<UUID, String> nombreCategoria = new HashMap<>();
        for (Category categoria : elegidas) {
            categorias.add(categoria.getId());
            nombreCategoria.put(categoria.getId(), categoria.getName());
        }
        if (categorias.isEmpty()) {
            return vistas;
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
        for (Partido partido : delDia) {
            String local = nombreEquipo.getOrDefault(partido.getIdEquipoLocal(), "Por definir");
            String visita = nombreEquipo.getOrDefault(partido.getIdEquipoVisita(), "Por definir");
            int equipos = 0;
            if (partido.getIdEquipoLocal() != null) {
                equipos++;
            }
            if (partido.getIdEquipoVisita() != null) {
                equipos++;
            }
            Category categoria = categoriaPartido(elegidas, partido.getIdCategoria());
            vistas.put(partido.getId(), new Vista(
                    textoHora(partido.getHora()),
                    nombreCategoria.getOrDefault(partido.getIdCategoria(), ""),
                    nombreJornada(porJornada.get(partido.getIdJornada())),
                    local + " VS " + visita,
                    equipos,
                    categoria == null ? cero() : monto(categoria.getValorPagoPartido()),
                    categoria == null ? cero() : monto(categoria.getValorPagoSinPito())));
        }
        return vistas;
    }

    private Linea linea(Vista vista, boolean pito) {
        Linea linea = new Linea();
        linea.hora = vista.hora;
        linea.categoria = vista.categoria;
        linea.jornada = vista.jornada;
        linea.partido = vista.partido;
        linea.pito = pito;
        linea.equipos = vista.equipos;
        linea.valorPagoPartido = vista.valorPagoPartido;
        linea.valorPagoSinPito = vista.valorPagoSinPito;
        linea.pago = cero();
        return linea;
    }

    private Category categoriaPartido(List<Category> elegidas, UUID categoryId) {
        for (Category categoria : elegidas) {
            if (categoria.getId().equals(categoryId)) {
                return categoria;
            }
        }
        return null;
    }

    private BigDecimal pago(Linea linea) {
        if (linea.pito) {
            return monto(linea.valorPagoPartido).multiply(BigDecimal.valueOf(linea.equipos));
        }
        return monto(linea.valorPagoSinPito);
    }

    private String nombre(Arbitro arbitro) {
        if (arbitro == null || arbitro.getNombre() == null || arbitro.getNombre().isBlank()) {
            return "Árbitro sin nombre";
        }
        return arbitro.getNombre();
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

    private String tablas(List<Grupo> grupos) {
        StringBuilder html = new StringBuilder();
        for (Grupo grupo : grupos) {
            html.append(tablaArbitro(grupo));
        }
        html.append(tablaResumen(grupos));
        return html.toString();
    }

    private String tablaArbitro(Grupo grupo) {
        StringBuilder cuerpo = new StringBuilder();
        BigDecimal total = cero();
        int numero = 1;
        for (Linea linea : grupo.lineas) {
            cuerpo.append("<tr>");
            cuerpo.append("<td class=\"numero\">").append(numero++).append("</td>");
            cuerpo.append("<td>").append(esc(linea.hora == null ? "Sin hora" : linea.hora)).append("</td>");
            cuerpo.append("<td>").append(esc(linea.categoria)).append("</td>");
            cuerpo.append("<td>").append(esc(linea.jornada)).append("</td>");
            cuerpo.append("<td>").append(esc(linea.partido)).append("</td>");
            cuerpo.append("<td class=\"pito\">").append(linea.pito ? "Sí" : "No").append("</td>");
            celda(cuerpo, linea.pago);
            cuerpo.append("</tr>");
            total = total.add(linea.pago);
        }
        cuerpo.append("<tr class=\"total\"><td colspan=\"6\">Total</td>");
        celda(cuerpo, total);
        cuerpo.append("</tr>");
        return tabla(grupo.nombre,
                "<th class=\"numero\">N°</th><th>Hora</th><th>Categoría</th><th>Jornada</th><th>Partido</th>"
                        + "<th class=\"pito\">Pitó</th><th class=\"monto\">Pago</th>",
                cuerpo.toString(), grupo.lineas.size());
    }

    private String tablaResumen(List<Grupo> grupos) {
        StringBuilder cuerpo = new StringBuilder();
        int partidos = 0;
        BigDecimal total = cero();
        int numero = 1;
        for (Grupo grupo : grupos) {
            BigDecimal pago = cero();
            for (Linea linea : grupo.lineas) {
                pago = pago.add(linea.pago);
            }
            cuerpo.append("<tr>");
            cuerpo.append("<td class=\"numero\">").append(numero++).append("</td>");
            cuerpo.append("<td>").append(esc(grupo.nombre)).append("</td>");
            cuerpo.append("<td class=\"monto\">").append(grupo.lineas.size()).append("</td>");
            celda(cuerpo, pago);
            cuerpo.append("</tr>");
            partidos += grupo.lineas.size();
            total = total.add(pago);
        }
        cuerpo.append("<tr class=\"total\"><td colspan=\"2\">Total</td>");
        cuerpo.append("<td class=\"monto\">").append(partidos).append("</td>");
        celda(cuerpo, total);
        cuerpo.append("</tr>");
        return tabla("RESUMEN",
                "<th class=\"numero\">N°</th><th>Árbitro</th><th class=\"monto\">Partidos</th><th class=\"monto\">Total</th>",
                cuerpo.toString(), grupos.size());
    }

    private String tabla(String titulo, String columnas, String cuerpo, int filas) {
        String clase = filas + 1 > FILAS_EN_HOJA ? "bloque salta" : "bloque";
        return "<div class=\"" + clase + "\"><h2>" + esc(titulo) + "</h2><table><thead><tr>" + columnas
                + "</tr></thead><tbody>" + cuerpo + "</tbody></table></div>";
    }

    private void celda(StringBuilder html, BigDecimal valor) {
        html.append("<td class=\"monto\">").append(dinero(valor)).append("</td>");
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
                + "td.numero, th.numero, td.pito, th.pito { text-align: center; }"
                + "td.numero, th.numero { width: 8mm; }"
                + "tr.total td { font-weight: 700; background: #163a5f; color: white; }"
                + "</style></head><body><div class=\"cabeza\">" + logo
                + "<h1>" + esc(liga.getName()) + "</h1>"
                + "<p class=\"sub\">PAGO ÁRBITROS</p>"
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

    private static final class Vista {
        private final String hora;
        private final String categoria;
        private final String jornada;
        private final String partido;
        private final int equipos;
        private final BigDecimal valorPagoPartido;
        private final BigDecimal valorPagoSinPito;

        private Vista(String hora, String categoria, String jornada, String partido, int equipos,
                      BigDecimal valorPagoPartido, BigDecimal valorPagoSinPito) {
            this.hora = hora;
            this.categoria = categoria;
            this.jornada = jornada;
            this.partido = partido;
            this.equipos = equipos;
            this.valorPagoPartido = valorPagoPartido;
            this.valorPagoSinPito = valorPagoSinPito;
        }
    }

    private static final class Linea {
        private String hora;
        private String categoria;
        private String jornada;
        private String partido;
        private boolean pito;
        private int equipos;
        private BigDecimal valorPagoPartido;
        private BigDecimal valorPagoSinPito;
        private BigDecimal pago;
    }

    private static final class Grupo {
        private final String nombre;
        private final List<Linea> lineas;

        private Grupo(String nombre, List<Linea> lineas) {
            this.nombre = nombre;
            this.lineas = lineas;
        }
    }
}
