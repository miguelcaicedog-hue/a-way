package com.away.core.competition;

import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReporteGoleadoresService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static final String[] MESES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };
    private static final int ALTO_PAGINA = 190;
    private static final int ALTO_CABECERA = 47;
    private static final int ALTO_TABLA = 36;
    private static final int ALTO_FILA = 6;

    private final GoleadorService goleadores;
    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;

    public ReporteGoleadoresService(GoleadorService goleadores, EditionRepository editions,
                                    CategoryRepository categories, JornadaRepository jornadas) {
        this.goleadores = goleadores;
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
    }

    @Transactional(readOnly = true)
    public byte[] pdf(UUID editionId, UUID categoryId, LocalDate fecha, UUID teamId, String numero, String nombre) {
        Preparado preparado = preparar(editionId, categoryId, fecha, teamId, numero, nombre);
        StringBuilder cuerpo = new StringBuilder();
        if (preparado.filas.isEmpty()) {
            cuerpo.append("<p class=\"vacio\">No hay goleadores con ese filtro.</p>");
        } else {
            cuerpo.append("<table><thead><tr>")
                    .append("<th class=\"numero\">Posición</th>")
                    .append("<th>Nombre</th><th>Equipo</th><th class=\"numero\">Goles</th>")
                    .append("</tr></thead><tbody>");
            int posicion = 1;
            for (GoleadorViews.GoleadorView goleador : preparado.filas) {
                cuerpo.append("<tr><td class=\"numero\">").append(posicion++).append("</td>")
                        .append("<td>").append(esc(etiqueta(goleador))).append("</td>")
                        .append("<td>").append(esc(goleador.getEquipo())).append("</td>")
                        .append("<td class=\"numero\">").append(goleador.getGoles()).append("</td></tr>");
            }
            cuerpo.append("</tbody></table>");
        }
        return ReportePdf.deHtml(documento(preparado.liga, "GOLEADORES", preparado.subtitulo, cuerpo.toString(), false), 210, 297);
    }

    @Transactional(readOnly = true)
    public byte[] detalle(UUID editionId, UUID categoryId, LocalDate fecha, UUID teamId, String numero, String nombre) {
        Preparado preparado = preparar(editionId, categoryId, fecha, teamId, numero, nombre);
        List<Jornada> columnas = columnas(categoryId, fecha, preparado.filas);
        StringBuilder cuerpo = new StringBuilder();
        if (preparado.filas.isEmpty()) {
            cuerpo.append("<p class=\"vacio\">No hay goleadores con ese filtro.</p>");
        } else {
            cuerpo.append("<h2>").append(esc(preparado.categoria.getName())).append("</h2>");
            Map<UUID, List<GoleadorViews.GoleadorView>> porEquipo = new LinkedHashMap<>();
            for (GoleadorViews.GoleadorView goleador : preparado.filas) {
                porEquipo.computeIfAbsent(goleador.getEquipoId(), id -> new ArrayList<>()).add(goleador);
            }
            int usado = ALTO_CABECERA;
            for (List<GoleadorViews.GoleadorView> equipo : porEquipo.values()) {
                int inicio = 0;
                while (inicio < equipo.size()) {
                    int libre = ALTO_PAGINA - usado;
                    int slots = (libre - ALTO_TABLA) / ALTO_FILA;
                    int slotsPagina = (ALTO_PAGINA - ALTO_TABLA) / ALTO_FILA;
                    int restantes = equipo.size() - inicio;
                    boolean tope = usado == 0 || usado == ALTO_CABECERA;
                    boolean salto = false;
                    int cupo;
                    if (restantes + 1 <= slots) {
                        cupo = restantes;
                    } else if (!tope && restantes + 1 <= slotsPagina) {
                        salto = true;
                        usado = 0;
                        cupo = restantes;
                    } else {
                        if (slots < 1) {
                            salto = true;
                            usado = 0;
                            slots = slotsPagina;
                        }
                        cupo = Math.min(restantes, slots);
                        if (cupo >= restantes) {
                            cupo = Math.max(1, slots - 1);
                        }
                    }
                    int fin = Math.min(inicio + cupo, equipo.size());
                    boolean cierre = fin == equipo.size();
                    cuerpo.append(tablaEquipo(equipo.subList(inicio, fin), equipo, columnas, salto || inicio > 0, cierre));
                    usado += ALTO_TABLA + (fin - inicio + (cierre ? 1 : 0)) * ALTO_FILA;
                    inicio = fin;
                }
            }
        }
        return ReportePdf.deHtml(documento(preparado.liga, "REPORTE DE GOLEADORES", preparado.subtitulo, cuerpo.toString(), true));
    }

    private Preparado preparar(UUID editionId, UUID categoryId, LocalDate fecha, UUID teamId, String numero, String nombre) {
        Edition edition = editions.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
        Category categoria = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        GoleadorViews.PanelView panel = goleadores.listar(editionId, categoryId, fecha, teamId);
        String camisa = numero == null ? "" : numero.trim();
        String buscado = nombre == null ? "" : nombre.trim().toLowerCase(Locale.ROOT);
        List<GoleadorViews.GoleadorView> filas = panel.getGoleadores().stream()
                .filter(goleador -> coincide(goleador, camisa, buscado))
                .collect(Collectors.toList());
        Preparado preparado = new Preparado();
        preparado.liga = edition.getLeague();
        preparado.categoria = categoria;
        preparado.filas = filas;
        preparado.subtitulo = subtitulo(categoria, fecha, panel, teamId, camisa, nombre);
        return preparado;
    }

    private List<Jornada> columnas(UUID categoryId, LocalDate fecha, List<GoleadorViews.GoleadorView> filas) {
        Set<String> conGoles = new HashSet<>();
        for (GoleadorViews.GoleadorView goleador : filas) {
            for (GoleadorViews.DetalleView item : goleador.getDetalle()) {
                conGoles.add(item.getJornada());
            }
        }
        List<Jornada> columnas = new ArrayList<>();
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoryId, "ACTIVO")) {
            boolean enCorte = jornada.getFecha() != null && !jornada.getFecha().isAfter(fecha);
            boolean sinFechaConGoles = jornada.getFecha() == null && conGoles.contains(jornada.getNombre());
            if (enCorte || sinFechaConGoles) {
                columnas.add(jornada);
            }
        }
        return columnas;
    }

    private String tablaEquipo(List<GoleadorViews.GoleadorView> equipo, List<GoleadorViews.GoleadorView> todos,
                                List<Jornada> columnas, boolean salto, boolean cierre) {
        StringBuilder html = new StringBuilder();
        int ancho = 1 + columnas.size() + 1;
        html.append(salto ? "<table class=\"corte\">" : "<table>");
        html.append("<colgroup>");
        int jugador = "Jugador".length();
        for (GoleadorViews.GoleadorView goleador : equipo) {
            jugador = Math.max(jugador, nombrePropio(etiqueta(goleador)).length());
        }
        html.append("<col style=\"width:").append(milimetros(jugador)).append("mm\"/>");
        for (int i = 0; i < columnas.size(); i++) {
            html.append("<col style=\"width:8mm\"/>");
        }
        html.append("<col style=\"width:14mm\"/></colgroup>");
        html.append("<thead><tr><th class=\"equipo\" colspan=\"").append(ancho).append("\">")
                .append(esc(equipo.get(0).getEquipo())).append("</th></tr><tr>");
        html.append("<th class=\"jugador\"></th>");
        int indice = 0;
        while (indice < columnas.size()) {
            LocalDate dia = columnas.get(indice).getFecha();
            int grupo = 1;
            while (indice + grupo < columnas.size() && mismoDia(dia, columnas.get(indice + grupo).getFecha())) {
                grupo++;
            }
            html.append("<th class=\"fecha\" colspan=\"").append(grupo).append("\">")
                    .append(dia == null ? "" : fechaCorta(dia)).append("</th>");
            indice += grupo;
        }
        html.append("<th class=\"total\"></th></tr><tr>");
        html.append("<th class=\"jugador\">Jugador</th>");
        for (Jornada jornada : columnas) {
            html.append("<th class=\"jornada\"><div class=\"giro\">")
                    .append(esc(nombrePropio(jornada.getNombre()))).append("</div></th>");
        }
        html.append("<th class=\"total\">Total</th></tr></thead><tbody>");
        for (GoleadorViews.GoleadorView goleador : equipo) {
            Map<String, Integer> porJornada = new HashMap<>();
            for (GoleadorViews.DetalleView item : goleador.getDetalle()) {
                porJornada.merge(item.getJornada(), item.getCantidad(), Integer::sum);
            }
            html.append("<tr><td class=\"jugador\">").append(esc(nombrePropio(etiqueta(goleador)))).append("</td>");
            int total = 0;
            for (Jornada jornada : columnas) {
                int goles = porJornada.getOrDefault(jornada.getNombre(), 0);
                total += goles;
                html.append("<td class=\"cuadro\">").append(goles).append("</td>");
            }
            html.append("<td class=\"total\">").append(total).append("</td></tr>");
        }
        if (cierre) {
            Map<String, Integer> porJornada = new HashMap<>();
            for (GoleadorViews.GoleadorView goleador : todos) {
                for (GoleadorViews.DetalleView item : goleador.getDetalle()) {
                    porJornada.merge(item.getJornada(), item.getCantidad(), Integer::sum);
                }
            }
            html.append("<tr><td class=\"total\">Total</td>");
            int total = 0;
            for (Jornada jornada : columnas) {
                int goles = porJornada.getOrDefault(jornada.getNombre(), 0);
                total += goles;
                html.append("<td class=\"total\">").append(goles).append("</td>");
            }
            html.append("<td class=\"total\">").append(total).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private boolean coincide(GoleadorViews.GoleadorView goleador, String camisa, String buscado) {
        if (!camisa.isEmpty() && !String.valueOf(goleador.getNumeroCamisa()).contains(camisa)) {
            return false;
        }
        if (buscado.isEmpty()) {
            return true;
        }
        return etiqueta(goleador).toLowerCase(Locale.ROOT).contains(buscado)
                || (goleador.getNombre() != null && goleador.getNombre().toLowerCase(Locale.ROOT).contains(buscado));
    }

    private String etiqueta(GoleadorViews.GoleadorView goleador) {
        String alias = goleador.getAlias() == null ? "" : goleador.getAlias().trim();
        return alias.isEmpty() ? goleador.getNombre() : alias;
    }

    private String subtitulo(Category categoria, LocalDate fecha, GoleadorViews.PanelView panel, UUID teamId,
                             String camisa, String nombre) {
        StringBuilder texto = new StringBuilder();
        texto.append(categoria.getName()).append(" · ").append(fecha.format(DIA));
        if (teamId != null) {
            String equipo = panel.getEquipos().stream()
                    .filter(item -> teamId.equals(item.getId()))
                    .map(GoleadorViews.EquipoView::getNombre)
                    .findFirst()
                    .orElse("");
            if (!equipo.isEmpty()) {
                texto.append(" · ").append(equipo);
            }
        }
        if (!camisa.isEmpty()) {
            texto.append(" · Nº ").append(camisa);
        }
        if (nombre != null && !nombre.trim().isEmpty()) {
            texto.append(" · ").append(nombre.trim());
        }
        return texto.toString();
    }

    private int milimetros(int caracteres) {
        return caracteres * 2 + 4;
    }

    private String fechaCorta(LocalDate dia) {
        return String.format("%02d %s", dia.getDayOfMonth(), MESES[dia.getMonthValue() - 1]);
    }

    private String nombrePropio(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        Locale idioma = Locale.forLanguageTag("es");
        StringBuilder nombre = new StringBuilder();
        for (String parte : valor.toLowerCase(idioma).split(" ")) {
            if (parte.isEmpty()) {
                continue;
            }
            if (nombre.length() > 0) {
                nombre.append(' ');
            }
            nombre.append(parte.substring(0, 1).toUpperCase(idioma)).append(parte.substring(1));
        }
        return nombre.toString();
    }

    private boolean mismoDia(LocalDate izquierda, LocalDate derecha) {
        return izquierda == null ? derecha == null : izquierda.equals(derecha);
    }

    private String documento(League liga, String titulo, String subtitulo, String cuerpo, boolean detalle) {
        String logo = "";
        if (liga.getLogo() != null && liga.getLogo().length > 0) {
            String tipo = liga.getLogoContentType() == null ? "image/png" : liga.getLogoContentType();
            logo = "<img class=\"logo\" src=\"data:" + tipo + ";base64,"
                    + Base64.getEncoder().encodeToString(liga.getLogo()) + "\" alt=\"\"/>";
        }
        String pagina = detalle
                ? "@page { size: 297mm 210mm; margin: 10mm 8mm 10mm; @bottom-center { content: counter(page); font-size: 9px; } }"
                : "@page { size: 210mm 297mm; margin: 10mm 8mm 12mm; @bottom-center { content: counter(page); font-size: 9px; } }";
        String tablas = detalle
                ? "table { width: auto; border-collapse: collapse; table-layout: fixed; margin: 0 0 8px; page-break-inside: avoid; }"
                        + "table.corte { page-break-before: always; }"
                        + "h2 { font-size: 13px; margin: 0 0 6px; page-break-after: avoid; }"
                        + "th.fecha { font-size: 7px; padding: 2px; text-align: center; background: #2f6f9f; }"
                        + "th.jornada { background: #e7f0f8; color: #163a5f; height: 24mm; padding: 0; text-align: center; vertical-align: middle; }"
                        + ".giro { display: inline-block; white-space: nowrap; font-size: 8px; font-weight: 700; transform: rotate(-90deg); }"
                        + "th.equipo, th.jugador, td.jugador, th.numero, td.numero { white-space: nowrap; }"
                        + "td.cuadro { text-align: center; font-weight: 700; font-size: 8px; }"
                        + "th.total, td.total { text-align: center; font-weight: 700; background: #163a5f; color: white; }"
                : "table { width: 100%; border-collapse: collapse; }"
                        + "td.numero, th.numero { text-align: center; width: 18mm; }";
        return "<html><head><meta charset=\"utf-8\"/><style>"
                + pagina
                + "body { font-family: sans-serif; font-size: 10px; color: #163a5f; }"
                + ".cabeza { text-align: center; margin-bottom: 12px; }"
                + ".logo { height: " + (detalle ? "58px" : "18mm") + "; }"
                + "h1 { font-size: 16px; margin: 6px 0 0; }"
                + "p.sub { margin: 2px 0 0; font-weight: 700; }"
                + "p.generado { margin: 2px 0 0; font-size: 9px; }"
                + "p.vacio { font-weight: 700; }"
                + tablas
                + "thead { display: table-header-group; }"
                + "tr { page-break-inside: avoid; }"
                + "th, td { border: 1px solid #163a5f; padding: 3px 4px; vertical-align: middle; }"
                + "th { background: #163a5f; color: white; }"
                + "</style></head><body><div class=\"cabeza\">" + logo
                + "<h1>" + esc(liga.getName()) + "</h1>"
                + "<p class=\"sub\">" + esc(titulo) + "</p>"
                + "<p class=\"sub\">" + esc(subtitulo) + "</p>"
                + "<p class=\"generado\">" + ZonedDateTime.now(ZONA).format(GENERADO) + "</p></div>"
                + cuerpo + "</body></html>";
    }

    private String esc(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class Preparado {
        private League liga;
        private Category categoria;
        private String subtitulo;
        private List<GoleadorViews.GoleadorView> filas;
    }
}
