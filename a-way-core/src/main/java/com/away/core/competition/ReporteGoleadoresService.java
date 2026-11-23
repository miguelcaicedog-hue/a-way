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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReporteGoleadoresService {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    private final GoleadorService goleadores;
    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final PlayerRepository jugadores;

    public ReporteGoleadoresService(GoleadorService goleadores, EditionRepository editions,
                                    CategoryRepository categories, PlayerRepository jugadores) {
        this.goleadores = goleadores;
        this.editions = editions;
        this.categories = categories;
        this.jugadores = jugadores;
    }

    @Transactional(readOnly = true)
    public byte[] pdf(UUID editionId, UUID categoryId, LocalDate fecha, UUID teamId, String numero, String nombre) {
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
        Map<UUID, Player> porId = new HashMap<>();
        if (!filas.isEmpty()) {
            for (Player jugador : jugadores.findAllById(filas.stream()
                    .map(GoleadorViews.GoleadorView::getJugadorId)
                    .collect(Collectors.toList()))) {
                porId.put(jugador.getId(), jugador);
            }
        }
        StringBuilder cuerpo = new StringBuilder();
        if (filas.isEmpty()) {
            cuerpo.append("<p class=\"vacio\">No hay goleadores con ese filtro.</p>");
        } else {
            cuerpo.append("<table><thead><tr>")
                    .append("<th class=\"numero\">Posición</th><th>Foto</th><th class=\"numero\">Nº</th>")
                    .append("<th>Nombre</th><th>Equipo</th><th class=\"numero\">Goles</th>")
                    .append("</tr></thead><tbody>");
            int posicion = 1;
            for (GoleadorViews.GoleadorView goleador : filas) {
                cuerpo.append("<tr><td class=\"numero\">").append(posicion++).append("</td>")
                        .append("<td class=\"foto\">").append(foto(porId.get(goleador.getJugadorId()))).append("</td>")
                        .append("<td class=\"numero\">").append(goleador.getNumeroCamisa()).append("</td>")
                        .append("<td>").append(esc(etiqueta(goleador))).append("</td>")
                        .append("<td>").append(esc(goleador.getEquipo())).append("</td>")
                        .append("<td class=\"numero\">").append(goleador.getGoles()).append("</td></tr>");
            }
            cuerpo.append("</tbody></table>");
        }
        return ReportePdf.deHtml(documento(edition.getLeague(), subtitulo(categoria, fecha, panel, teamId, camisa, nombre), cuerpo.toString()), 210, 297);
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

    private String foto(Player jugador) {
        if (jugador == null) {
            return "";
        }
        byte[] bytes = jugador.getPhoto();
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String tipo = tipoFoto(jugador.getPhotoContentType());
        if (tipo == null) {
            return "";
        }
        String giro = jugador.getRotationDegrees() == 0
                ? ""
                : " style=\"transform:rotate(" + jugador.getRotationDegrees() + "deg)\"";
        return "<img class=\"retrato\" src=\"data:" + tipo + ";base64,"
                + Base64.getEncoder().encodeToString(bytes) + "\" alt=\"\"" + giro + "/>";
    }

    private String tipoFoto(String tipo) {
        if (tipo == null) {
            return "image/jpeg";
        }
        String normalizado = tipo.toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalizado) || "image/pjpeg".equals(normalizado)) {
            return "image/jpeg";
        }
        if ("image/jpeg".equals(normalizado) || "image/png".equals(normalizado) || "image/gif".equals(normalizado)) {
            return normalizado;
        }
        return null;
    }

    private String documento(League liga, String subtitulo, String cuerpo) {
        String logo = "";
        if (liga.getLogo() != null && liga.getLogo().length > 0) {
            String tipo = liga.getLogoContentType() == null ? "image/png" : liga.getLogoContentType();
            logo = "<img class=\"logo\" src=\"data:" + tipo + ";base64,"
                    + Base64.getEncoder().encodeToString(liga.getLogo()) + "\" alt=\"\"/>";
        }
        return "<html><head><meta charset=\"utf-8\"/><style>"
                + "@page { size: 210mm 297mm; margin: 10mm 8mm 12mm; @bottom-center { content: counter(page); font-size: 9px; } }"
                + "body { font-family: sans-serif; font-size: 10px; color: #163a5f; }"
                + ".cabeza { text-align: center; margin-bottom: 12px; }"
                + ".logo { height: 18mm; }"
                + "h1 { font-size: 16px; margin: 6px 0 0; }"
                + "p.sub { margin: 2px 0 0; font-weight: 700; }"
                + "p.generado { margin: 2px 0 0; font-size: 9px; }"
                + "p.vacio { font-weight: 700; }"
                + "table { width: 100%; border-collapse: collapse; }"
                + "thead { display: table-header-group; }"
                + "tr { page-break-inside: avoid; }"
                + "th, td { border: 1px solid #163a5f; padding: 3px 4px; vertical-align: middle; }"
                + "th { background: #163a5f; color: white; }"
                + "td.numero, th.numero { text-align: center; width: 18mm; }"
                + "td.foto { text-align: center; width: 18mm; height: 16mm; }"
                + "img.retrato { max-height: 14mm; max-width: 14mm; }"
                + "</style></head><body><div class=\"cabeza\">" + logo
                + "<h1>" + esc(liga.getName()) + "</h1>"
                + "<p class=\"sub\">GOLEADORES</p>"
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
