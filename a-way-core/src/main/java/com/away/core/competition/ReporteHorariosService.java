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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReporteHorariosService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TITULO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "EC"));
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static final int FILAS_EN_HOJA = 24;

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final HorarioService horarios;

    public ReporteHorariosService(EditionRepository editions, CategoryRepository categories, HorarioService horarios) {
        this.editions = editions;
        this.categories = categories;
        this.horarios = horarios;
    }

    @Transactional(readOnly = true)
    public byte[] horarios(UUID editionId, UUID categoryId, LocalDate fecha) {
        Edition edition = editions.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
        HorarioViews.HorarioPanelView panel = horarios.panel(editionId, categoryId, fecha, null, null);
        String subtitulo = fecha == null ? "Todas las fechas" : fecha.format(DIA);
        if (categoryId != null) {
            Category categoria = categories.findById(categoryId).orElse(null);
            if (categoria != null) {
                subtitulo = subtitulo + " · " + categoria.getName();
            }
        }
        return ReportePdf.deHtml(documento(edition.getLeague(), subtitulo, cuerpo(panel, categoryId == null)));
    }

    private String cuerpo(HorarioViews.HorarioPanelView panel, boolean muestraCategoria) {
        List<HorarioViews.HorarioPartidoView> asignados = new ArrayList<>();
        List<HorarioViews.HorarioPartidoView> pendientes = new ArrayList<>();
        for (HorarioViews.HorarioPartidoView partido : panel.getPartidos()) {
            if (partido.getFecha() != null && partido.getHora() != null && partido.getCanchaId() != null) {
                asignados.add(partido);
            } else {
                pendientes.add(partido);
            }
        }
        if (asignados.isEmpty() && pendientes.isEmpty()) {
            return "<p class=\"vacio\">No hay partidos.</p>";
        }
        StringBuilder html = new StringBuilder();
        html.append(tablasAsignadas(asignados, muestraCategoria));
        if (!pendientes.isEmpty()) {
            html.append(tablaPendientes(pendientes, muestraCategoria));
        }
        return html.toString();
    }

    private String tablasAsignadas(List<HorarioViews.HorarioPartidoView> asignados, boolean muestraCategoria) {
        Map<LocalDate, List<HorarioViews.HorarioPartidoView>> porDia = new LinkedHashMap<>();
        asignados.sort(Comparator.comparing(HorarioViews.HorarioPartidoView::getFecha));
        for (HorarioViews.HorarioPartidoView partido : asignados) {
            porDia.computeIfAbsent(partido.getFecha(), dia -> new ArrayList<>()).add(partido);
        }
        StringBuilder html = new StringBuilder();
        for (Map.Entry<LocalDate, List<HorarioViews.HorarioPartidoView>> dia : porDia.entrySet()) {
            html.append(tablaDia(dia.getKey(), dia.getValue(), muestraCategoria));
        }
        return html.toString();
    }

    private String tablaDia(LocalDate dia, List<HorarioViews.HorarioPartidoView> partidos, boolean muestraCategoria) {
        partidos.sort(Comparator
                .comparingInt(HorarioViews.HorarioPartidoView::getJornadaNumero)
                .thenComparing(HorarioViews.HorarioPartidoView::getCategoryName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(HorarioViews.HorarioPartidoView::getHora, Comparator.nullsLast(String::compareTo))
                .thenComparing(partido -> partido.getCanchaNombre() == null ? "" : partido.getCanchaNombre(),
                        String.CASE_INSENSITIVE_ORDER));
        StringBuilder cuerpo = new StringBuilder();
        int inicio = 0;
        while (inicio < partidos.size()) {
            HorarioViews.HorarioPartidoView primero = partidos.get(inicio);
            int fin = inicio + 1;
            while (fin < partidos.size() && mismaJornada(primero, partidos.get(fin))) {
                fin++;
            }
            int span = fin - inicio;
            for (int indice = inicio; indice < fin; indice++) {
                HorarioViews.HorarioPartidoView partido = partidos.get(indice);
                cuerpo.append("<tr>");
                if (indice == inicio) {
                    cuerpo.append("<td class=\"grupo\" rowspan=\"").append(span).append("\">")
                            .append(esc(partido.getJornadaNombre())).append("</td>");
                }
                cuerpo.append("<td>").append(esc(partido.getHora())).append("</td>");
                cuerpo.append("<td>").append(esc(partido.getCanchaNombre())).append("</td>");
                cuerpo.append("<td class=\"numero\">").append(indice - inicio + 1).append("</td>");
                if (muestraCategoria) {
                    cuerpo.append("<td>").append(esc(partido.getCategoryName())).append("</td>");
                }
                cuerpo.append("<td>").append(esc(partido.getTextoLocal())).append("</td>");
                cuerpo.append("<td>").append(esc(partido.getTextoVisita())).append("</td>");
                cuerpo.append("</tr>");
            }
            inicio = fin;
        }
        String columnas = "<th>Jornada</th><th>Hora</th><th>Cancha</th><th class=\"numero\">N°</th>"
                + (muestraCategoria ? "<th>Categoría</th>" : "")
                + "<th>Local</th><th>Visita</th>";
        return tabla(dia.format(TITULO).toUpperCase(new Locale("es", "EC")), columnas, cuerpo.toString(), partidos.size());
    }

    private boolean mismaJornada(HorarioViews.HorarioPartidoView izquierda, HorarioViews.HorarioPartidoView derecha) {
        return izquierda.getJornadaNumero() == derecha.getJornadaNumero()
                && misma(izquierda.getCategoryId(), derecha.getCategoryId());
    }

    private boolean misma(UUID izquierda, UUID derecha) {
        return izquierda == null ? derecha == null : izquierda.equals(derecha);
    }

    private String tablaPendientes(List<HorarioViews.HorarioPartidoView> pendientes, boolean muestraCategoria) {
        pendientes.sort(Comparator
                .comparingInt(HorarioViews.HorarioPartidoView::getJornadaNumero)
                .thenComparing(HorarioViews.HorarioPartidoView::getCategoryName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(partido -> partido.getFecha() == null ? LocalDate.MAX : partido.getFecha())
                .thenComparingInt(HorarioViews.HorarioPartidoView::getOrden));
        StringBuilder cuerpo = new StringBuilder();
        int inicio = 0;
        while (inicio < pendientes.size()) {
            HorarioViews.HorarioPartidoView primero = pendientes.get(inicio);
            int fin = inicio + 1;
            while (fin < pendientes.size() && mismaJornada(primero, pendientes.get(fin))) {
                fin++;
            }
            int span = fin - inicio;
            for (int indice = inicio; indice < fin; indice++) {
                HorarioViews.HorarioPartidoView partido = pendientes.get(indice);
                cuerpo.append("<tr>");
                if (indice == inicio) {
                    cuerpo.append("<td class=\"grupo\" rowspan=\"").append(span).append("\">")
                            .append(esc(partido.getJornadaNombre())).append("</td>");
                }
                cuerpo.append("<td>").append(partido.getFecha() == null ? "Sin fecha" : partido.getFecha().format(DIA)).append("</td>");
                if (muestraCategoria) {
                    cuerpo.append("<td>").append(esc(partido.getCategoryName())).append("</td>");
                }
                cuerpo.append("<td>").append(esc(partido.getTextoLocal())).append("</td>");
                cuerpo.append("<td>").append(esc(partido.getTextoVisita())).append("</td>");
                cuerpo.append("</tr>");
            }
            inicio = fin;
        }
        String columnas = "<th>Jornada</th><th>Fecha</th>"
                + (muestraCategoria ? "<th>Categoría</th>" : "")
                + "<th>Local</th><th>Visita</th>";
        return tabla("SIN HORARIO", columnas, cuerpo.toString(), pendientes.size());
    }

    private String tabla(String titulo, String columnas, String cuerpo, int filas) {
        String clase = filas + 1 > FILAS_EN_HOJA ? "bloque salta" : "bloque";
        return "<div class=\"" + clase + "\"><h2>" + esc(titulo) + "</h2><table><thead><tr>" + columnas
                + "</tr></thead><tbody>" + cuerpo + "</tbody></table></div>";
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
                + "td.numero, th.numero { text-align: center; width: 8mm; }"
                + "td.grupo { vertical-align: middle; text-align: center; font-weight: 700; background: #eef3f8; }"
                + "</style></head><body><div class=\"cabeza\">" + logo
                + "<h1>" + esc(liga.getName()) + "</h1>"
                + "<p class=\"sub\">HORARIOS DE PARTIDOS</p>"
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
}
