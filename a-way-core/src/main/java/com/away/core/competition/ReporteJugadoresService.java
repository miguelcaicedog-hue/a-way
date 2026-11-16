package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReporteJugadoresService {

    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final PlayerRepository jugadores;
    private final AppUserRepository users;

    public ReporteJugadoresService(EditionRepository editions, CategoryRepository categories,
                                   PlayerRepository jugadores, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jugadores = jugadores;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public byte[] jugadoresPorEquipo(UUID editionId, UUID categoryId) {
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una categoría");
        }
        Edition edition = editions.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
        Category categoria = categories.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
        if (categoria.getEdition() == null || !edition.getId().equals(categoria.getEdition().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a la edición");
        }
        League liga = edition.getLeague();
        assertRead(liga.getId());
        Map<UUID, List<Player>> porEquipo = new LinkedHashMap<>();
        Map<UUID, String> nombres = new LinkedHashMap<>();
        for (Player jugador : jugadores.activosDeCategoria(categoryId)) {
            UUID equipoId = jugador.getTeamEntry().getTeam().getId();
            nombres.putIfAbsent(equipoId, jugador.getTeamEntry().getTeam().getName());
            porEquipo.computeIfAbsent(equipoId, id -> new ArrayList<>()).add(jugador);
        }
        StringBuilder cuerpo = new StringBuilder();
        if (porEquipo.isEmpty()) {
            cuerpo.append("<p class=\"vacio\">No hay jugadores activos en esta categoría.</p>");
        } else {
            int equipos = 0;
            for (Map.Entry<UUID, List<Player>> grupo : porEquipo.entrySet()) {
                cuerpo.append(htmlEquipo(nombres.get(grupo.getKey()), grupo.getValue(), equipos > 0 && equipos % 2 == 0));
                equipos++;
            }
        }
        return ReportePdf.deHtml(documento(liga, cuerpo.toString()), 210, 297);
    }

    private String htmlEquipo(String equipo, List<Player> jugadoresEquipo, boolean salto) {
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"equipo");
        if (salto) {
            html.append(" corte");
        }
        html.append("\"><colgroup>");
        for (int grupo = 0; grupo < 3; grupo++) {
            html.append("<col style=\"width:27mm\"/><col/><col style=\"width:12mm\"/>");
        }
        html.append("</colgroup><thead><tr><th class=\"equipo\" colspan=\"9\">").append(esc(equipo)).append("</th></tr><tr>");
        for (int grupo = 0; grupo < 3; grupo++) {
            html.append("<th>Foto</th><th>NOMBRE</th><th>Nº</th>");
        }
        html.append("</tr></thead><tbody>");
        for (int inicio = 0; inicio < jugadoresEquipo.size(); inicio += 3) {
            html.append("<tr>");
            for (int columna = 0; columna < 3; columna++) {
                int indice = inicio + columna;
                if (indice < jugadoresEquipo.size()) {
                    jugador(html, jugadoresEquipo.get(indice));
                } else {
                    html.append("<td class=\"foto\"></td><td class=\"nombre\"></td><td class=\"numero\"></td>");
                }
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private void jugador(StringBuilder html, Player jugador) {
        html.append("<td class=\"foto\">").append(foto(jugador)).append("</td>")
                .append("<td class=\"nombre\">").append(esc(nombrePropio(jugador.getAlias()))).append("</td>")
                .append("<td class=\"numero\">").append(jugador.getNumeroCamisa()).append("</td>");
    }

    private String foto(Player jugador) {
        byte[] bytes = jugador.getPhoto();
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String tipo = tipoFoto(jugador.getPhotoContentType());
        if (tipo == null) {
            return "";
        }
        String clase = jugador.getRotationDegrees() == 0 ? "retrato" : "retrato gira";
        String giro = jugador.getRotationDegrees() == 0
                ? ""
                : " style=\"transform:rotate(" + jugador.getRotationDegrees() + "deg)\"";
        return "<img class=\"" + clase + "\" src=\"data:" + tipo + ";base64,"
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

    private String documento(League liga, String cuerpo) {
        String logo = "";
        if (liga.getLogo() != null && liga.getLogo().length > 0) {
            String tipo = liga.getLogoContentType() == null ? "image/png" : liga.getLogoContentType();
            logo = "<img class=\"logo\" src=\"data:" + tipo + ";base64,"
                    + Base64.getEncoder().encodeToString(liga.getLogo()) + "\" alt=\"\"/>";
        }
        return "<html><head><meta charset=\"utf-8\"/><style>"
                + "@page { size: 210mm 297mm; margin: 10mm 8mm 10mm; @bottom-center { content: counter(page); font-size: 9px; } }"
                + "body { font-family: sans-serif; font-size: 10px; color: #163a5f; }"
                + "table.cabecera { width: 100%; border: none; margin-bottom: 10px; }"
                + "table.cabecera td { border: none; vertical-align: middle; padding: 0; }"
                + "td.marca { width: 28mm; text-align: left; }"
                + "td.textos { text-align: center; }"
                + ".logo { height: 18mm; }"
                + "h1 { font-size: 20px; margin: 0; text-align: center; }"
                + "p.sub { margin: 2px 0 0; font-size: 14px; font-weight: 700; text-align: center; }"
                + "p.generado { margin: 2px 0 0; font-size: 9px; text-align: center; }"
                + "table.equipo { width: 100%; border-collapse: collapse; table-layout: fixed; margin: 0 0 6px; page-break-inside: avoid; }"
                + "table.corte { page-break-before: always; }"
                + "th, td { border: 1px solid #163a5f; padding: 3px 4px; }"
                + "th { background: #163a5f; color: white; text-align: center; }"
                + "td.foto { text-align: center; height: 27mm; }"
                + "img.retrato { max-height: 25mm; max-width: 25mm; }"
                + "td.nombre { font-size: 11px; text-align: center; }"
                + "td.numero { text-align: center; font-size: 14px; font-weight: 700; }"
                + "tr { page-break-inside: avoid; }"
                + "</style></head><body>"
                + "<table class=\"cabecera\"><tr><td class=\"marca\">" + logo + "</td><td class=\"textos\">"
                + "<h1>REPORTE DE JUGADORES POR EQUIPO</h1>"
                + "<p class=\"sub\">" + esc(liga.getName()) + "</p>"
                + "<p class=\"generado\">" + ZonedDateTime.now(ZONA).format(GENERADO) + "</p>"
                + "</td><td class=\"marca\"></td></tr></table>"
                + cuerpo + "</body></html>";
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
}
