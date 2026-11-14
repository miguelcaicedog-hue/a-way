package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReporteTarjetasService {

    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final TarjetaJugadorRepository tarjetas;
    private final TarjetaRepository catalogo;
    private final PartidoCobroDetalleRepository cobros;
    private final SuspensionJugadorRepository suspensiones;
    private final PlayerRepository jugadores;
    private final AppUserRepository users;

    public ReporteTarjetasService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                                  PartidoRepository partidos, TarjetaJugadorRepository tarjetas, TarjetaRepository catalogo,
                                  PartidoCobroDetalleRepository cobros, SuspensionJugadorRepository suspensiones,
                                  PlayerRepository jugadores, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.tarjetas = tarjetas;
        this.catalogo = catalogo;
        this.cobros = cobros;
        this.suspensiones = suspensiones;
        this.jugadores = jugadores;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public byte[] tarjetasYSuspensiones(UUID editionId, UUID categoryId, LocalDate fecha) {
        if (fecha == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una fecha");
        }
        Edition edition = editions.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
        League liga = edition.getLeague();
        assertRead(liga.getId());
        List<Category> elegidas = new ArrayList<>();
        for (Category categoria : categories.findByEdition_IdAndEstadoOrderByNameAsc(edition.getId(), "ACTIVO")) {
            if (categoryId == null || categoryId.equals(categoria.getId())) {
                elegidas.add(categoria);
            }
        }
        Set<UUID> pagadas = new HashSet<>();
        for (PartidoCobroDetalle detalle : cobros.cargosActivos()) {
            if ("TARJETA".equals(detalle.getTipo()) && detalle.getIdTarjetaJugador() != null) {
                pagadas.add(detalle.getIdTarjetaJugador());
            }
        }
        Colores colores = colores(liga.getId());
        StringBuilder cuerpo = new StringBuilder();
        boolean alguna = false;
        for (Category categoria : elegidas) {
            Bloque bloque = bloque(categoria, fecha, pagadas, colores);
            if (bloque.filas.isEmpty()) {
                continue;
            }
            alguna = true;
            cuerpo.append(htmlBloque(bloque));
        }
        if (!alguna) {
            cuerpo.append("<p class=\"vacio\">No hay tarjetas en esta fecha.</p>");
        }
        return ReportePdf.deHtml(documento(liga, cuerpo.toString()));
    }

    private Bloque bloque(Category categoria, LocalDate fecha, Set<UUID> pagadas, Colores colores) {
        List<Jornada> orden = jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoria.getId(), "ACTIVO");
        Map<UUID, Jornada> porJornada = new HashMap<>();
        Map<UUID, Integer> indice = new HashMap<>();
        for (int i = 0; i < orden.size(); i++) {
            porJornada.put(orden.get(i).getId(), orden.get(i));
            indice.put(orden.get(i).getId(), i);
        }
        Map<UUID, Partido> porPartido = new HashMap<>();
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoria.getId(), "ACTIVO")) {
            porPartido.put(partido.getId(), partido);
        }
        List<SuspensionJugador> activas = suspensiones.findByIdCategoriaAndEstado(categoria.getId(), "ACTIVO");
        List<Fila> filas = new ArrayList<>();
        for (TarjetaJugador tarjeta : tarjetas.activasDeCategoria(categoria.getId())) {
            if ((tarjeta.getAmarillas() == null || tarjeta.getAmarillas() <= 0)
                    && (tarjeta.getRojas() == null || tarjeta.getRojas() <= 0)) {
                continue;
            }
            Partido partido = porPartido.get(tarjeta.getIdPartido());
            if (partido == null || !fecha.equals(fechaDe(partido, porJornada))) {
                continue;
            }
            Player jugador = jugadores.findById(tarjeta.getIdJugador()).orElse(null);
            if (jugador == null) {
                continue;
            }
            Fila fila = new Fila();
            fila.equipo = jugador.getTeamEntry() == null || jugador.getTeamEntry().getTeam() == null
                    ? "" : jugador.getTeamEntry().getTeam().getName();
            fila.jugador = jugador.getAlias() == null ? "" : jugador.getAlias();
            fila.numero = jugador.getNumeroCamisa();
            fila.idJornadaTarjeta = partido.getIdJornada();
            fila.amarillas = tarjeta.getAmarillas() == null ? 0 : tarjeta.getAmarillas();
            fila.rojas = tarjeta.getRojas() == null ? 0 : tarjeta.getRojas();
            fila.pagada = pagadas.contains(tarjeta.getId());
            fila.marcadas.addAll(jornadasSuspendidas(activas, tarjeta.getIdJugador(), orden, indice));
            fila.marcadas.remove(fila.idJornadaTarjeta);
            filas.add(fila);
        }
        filas.sort(Comparator.comparing((Fila fila) -> fila.equipo).thenComparing(fila -> fila.jugador));
        Set<UUID> usadas = new HashSet<>();
        for (Fila fila : filas) {
            if (fila.idJornadaTarjeta != null) {
                usadas.add(fila.idJornadaTarjeta);
            }
            usadas.addAll(fila.marcadas);
        }
        List<Jornada> columnas = new ArrayList<>();
        for (Jornada jornada : orden) {
            if (usadas.contains(jornada.getId())) {
                columnas.add(jornada);
            }
        }
        Bloque bloque = new Bloque();
        bloque.filas = filas;
        bloque.columnas = columnas;
        bloque.colores = colores;
        return bloque;
    }

    private List<UUID> jornadasSuspendidas(List<SuspensionJugador> activas, UUID jugadorId, List<Jornada> orden,
                                           Map<UUID, Integer> indice) {
        List<UUID> marcas = new ArrayList<>();
        for (SuspensionJugador suspension : activas) {
            if (!jugadorId.equals(suspension.getIdJugador())) {
                continue;
            }
            if ("FALTA_GRAVE".equals(suspension.getMotivo())) {
                Integer inicio = indice.get(suspension.getIdJornadaInicio());
                if (inicio == null) {
                    continue;
                }
                for (int i = inicio; i < orden.size(); i++) {
                    marcas.add(orden.get(i).getId());
                }
                continue;
            }
            Integer inicio = indice.get(suspension.getIdJornadaInicio());
            if (inicio == null) {
                continue;
            }
            for (int i = inicio; i < orden.size() && i < inicio + suspension.getJornadas(); i++) {
                marcas.add(orden.get(i).getId());
            }
        }
        return marcas;
    }

    private String htmlBloque(Bloque bloque) {
        StringBuilder html = new StringBuilder();
        html.append("<table>").append(columnas(bloque));
        html.append("<thead><tr><th class=\"equipo\" rowspan=\"3\">Equipo</th><th class=\"numero\" rowspan=\"3\">Nº</th><th class=\"jugador\" rowspan=\"3\">Jugador</th>");
        int indice = 0;
        while (indice < bloque.columnas.size()) {
            LocalDate dia = bloque.columnas.get(indice).getFecha();
            int ancho = 1;
            while (indice + ancho < bloque.columnas.size()
                    && mismoDia(dia, bloque.columnas.get(indice + ancho).getFecha())) {
                ancho++;
            }
            html.append("<th class=\"fecha\" colspan=\"").append(ancho * 2).append("\">")
                    .append(dia == null ? "" : fechaCorta(dia)).append("</th>");
            indice += ancho;
        }
        html.append("</tr><tr>");
        for (Jornada jornada : bloque.columnas) {
            html.append("<th class=\"jornada\" colspan=\"2\"><div class=\"giro\">")
                    .append(esc(nombrePropio(jornada.getNombre()))).append("</div></th>");
        }
        html.append("</tr><tr>");
        for (int i = 0; i < bloque.columnas.size(); i++) {
            html.append("<th class=\"muestra\" style=\"background:").append(cssColor(bloque.colores.amarilla)).append("\"></th>");
            html.append("<th class=\"muestra\" style=\"background:").append(cssColor(bloque.colores.roja)).append("\"></th>");
        }
        html.append("</tr></thead><tbody>");
        int filaIndice = 0;
        while (filaIndice < bloque.filas.size()) {
            Fila primera = bloque.filas.get(filaIndice);
            int grupo = 1;
            while (filaIndice + grupo < bloque.filas.size()
                    && java.util.Objects.equals(primera.equipo, bloque.filas.get(filaIndice + grupo).equipo)) {
                grupo++;
            }
            for (int desplazamiento = 0; desplazamiento < grupo; desplazamiento++) {
                Fila fila = bloque.filas.get(filaIndice + desplazamiento);
                html.append("<tr>");
                if (desplazamiento == 0) {
                    html.append("<td class=\"equipo\" rowspan=\"").append(grupo).append("\">")
                            .append(esc(fila.equipo)).append("</td>");
                }
                html.append("<td class=\"numero\">").append(fila.numero).append("</td>");
                html.append("<td class=\"jugador\">").append(esc(nombrePropio(fila.jugador))).append("</td>");
            int columna = 0;
            while (columna < bloque.columnas.size()) {
                Jornada jornada = bloque.columnas.get(columna);
                if (jornada.getId().equals(fila.idJornadaTarjeta)) {
                    celda(html, fila.amarillas, fila.pagada, bloque.colores.amarilla);
                    celda(html, fila.rojas, fila.pagada, bloque.colores.roja);
                    columna++;
                    continue;
                }
                if (fila.marcadas.contains(jornada.getId())) {
                    int ancho = 1;
                    while (columna + ancho < bloque.columnas.size()
                            && fila.marcadas.contains(bloque.columnas.get(columna + ancho).getId())) {
                        ancho++;
                    }
                    html.append("<td class=\"cuadro susp\" colspan=\"").append(ancho * 2).append("\">SUSP</td>");
                    columna += ancho;
                    continue;
                }
                html.append("<td class=\"cuadro\"></td><td class=\"cuadro\"></td>");
                columna++;
            }
            html.append("</tr>");
            }
            filaIndice += grupo;
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String columnas(Bloque bloque) {
        int equipo = "Equipo".length();
        int numero = 1;
        int jugador = "Jugador".length();
        for (Fila fila : bloque.filas) {
            equipo = Math.max(equipo, fila.equipo == null ? 0 : fila.equipo.length());
            numero = Math.max(numero, String.valueOf(fila.numero).length());
            jugador = Math.max(jugador, nombrePropio(fila.jugador).length());
        }
        StringBuilder grupo = new StringBuilder("<colgroup>");
        grupo.append("<col style=\"width:").append(milimetros(equipo)).append("mm\"/>");
        grupo.append("<col style=\"width:").append(milimetros(numero)).append("mm\"/>");
        grupo.append("<col style=\"width:").append(milimetros(jugador)).append("mm\"/>");
        for (int i = 0; i < bloque.columnas.size(); i++) {
            grupo.append("<col style=\"width:5.6mm\"/><col style=\"width:5.6mm\"/>");
        }
        grupo.append("</colgroup>");
        return grupo.toString();
    }

    private int milimetros(int caracteres) {
        return caracteres * 2 + 4;
    }

    private static final String[] MESES = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };

    private String fechaCorta(LocalDate dia) {
        return String.format("%02d %s", dia.getDayOfMonth(), MESES[dia.getMonthValue() - 1]);
    }

    private String nombrePropio(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        java.util.Locale idioma = java.util.Locale.forLanguageTag("es");
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

    private Colores colores(UUID ligaId) {
        String amarilla = "#FFFF00";
        String roja = "#FF0000";
        for (Tarjeta tarjeta : catalogo.buscar(ligaId, "", "ACTIVO")) {
            String nombre = tarjeta.getDescripcion() == null ? "" : tarjeta.getDescripcion().toUpperCase(java.util.Locale.ROOT);
            if (nombre.contains("AMARILL") && tarjeta.getColor() != null) {
                amarilla = tarjeta.getColor();
            }
            if (nombre.contains("ROJA") && tarjeta.getColor() != null) {
                roja = tarjeta.getColor();
            }
        }
        return new Colores(amarilla, roja);
    }

    private void celda(StringBuilder html, int cantidad, boolean pagada, String color) {
        if (cantidad <= 0) {
            html.append("<td class=\"cuadro\"></td>");
            return;
        }
        String fondo = cssColor(color);
        String marca = pagada ? cantidad + "P" : (cantidad > 1 ? String.valueOf(cantidad) : "");
        html.append("<td class=\"cuadro\" style=\"background:").append(fondo)
                .append(";color:").append(tinta(fondo)).append(";\">").append(marca).append("</td>");
    }

    private String cssColor(String color) {
        if (color != null && color.matches("#[0-9A-Fa-f]{6}")) {
            return color;
        }
        return "#FFFF00";
    }

    private String tinta(String hex) {
        int rojo = Integer.parseInt(hex.substring(1, 3), 16);
        int verde = Integer.parseInt(hex.substring(3, 5), 16);
        int azul = Integer.parseInt(hex.substring(5, 7), 16);
        int luz = (rojo * 299 + verde * 587 + azul * 114) / 1000;
        return luz < 140 ? "#ffffff" : "#163a5f";
    }

    private String documento(League liga, String cuerpo) {
        String logo = "";
        if (liga.getLogo() != null && liga.getLogo().length > 0) {
            String tipo = liga.getLogoContentType() == null ? "image/png" : liga.getLogoContentType();
            logo = "<img class=\"logo\" src=\"data:" + tipo + ";base64,"
                    + Base64.getEncoder().encodeToString(liga.getLogo()) + "\" alt=\"\"/>";
        }
        return "<html><head><meta charset=\"utf-8\"/><style>"
                + "@page { size: 297mm 210mm; margin: 10mm 8mm 10mm; @bottom-center { content: counter(page); font-size: 9px; } }"
                + "body { font-family: sans-serif; font-size: 10px; color: #163a5f; }"
                + ".cabeza { text-align: center; margin-bottom: 12px; }"
                + ".logo { height: 58px; }"
                + "h1 { font-size: 16px; margin: 6px 0 0; }"
                + "p.sub { margin: 2px 0 0; font-weight: 700; }"
                + "p.generado { margin: 2px 0 0; font-size: 9px; }"
                + "table { width: auto; border-collapse: collapse; table-layout: fixed; }"
                + "th, td { border: 1px solid #163a5f; padding: 3px 4px; }"
                + "th { background: #163a5f; color: white; }"
                + "th.fecha { font-size: 7px; padding: 2px; text-align: center; }"
                + "th.jornada { background: #e7f0f8; color: #163a5f; height: 24mm; padding: 0; text-align: center; vertical-align: middle; }"
                + ".giro { display: inline-block; white-space: nowrap; font-size: 8px; font-weight: 700; transform: rotate(-90deg); }"
                + "th.muestra { height: 8px; padding: 0; }"
                + "th.equipo, td.equipo, th.jugador, td.jugador, th.numero, td.numero { white-space: nowrap; }"
                + "td.numero, th.numero { text-align: center; }"
                + "th.fecha { background: #2f6f9f; }"
                + "td.equipo, td.jugador { white-space: nowrap; }"
                + "td.cuadro { text-align: center; font-weight: 700; font-size: 8px; }"
                + "td.susp { background: #f3c98b; color: #163a5f; }"
                + "</style></head><body><div class=\"cabeza\">" + logo
                + "<h1>" + esc(liga.getName()) + "</h1>"
                + "<p class=\"sub\">DETALLE DE TARJETAS Y SUSPENSIONES</p>"
                + "<p class=\"generado\">" + ZonedDateTime.now(ZONA).format(GENERADO) + "</p></div>"
                + cuerpo + "</body></html>";
    }

    private LocalDate fechaDe(Partido partido, Map<UUID, Jornada> porJornada) {
        if (partido.getFecha() != null) {
            return partido.getFecha();
        }
        Jornada jornada = porJornada.get(partido.getIdJornada());
        return jornada == null ? null : jornada.getFecha();
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

    private static final class Bloque {
        private List<Fila> filas = new ArrayList<>();
        private List<Jornada> columnas = new ArrayList<>();
        private Colores colores;
    }

    private static final class Colores {
        private final String amarilla;
        private final String roja;

        private Colores(String amarilla, String roja) {
            this.amarilla = amarilla;
            this.roja = roja;
        }
    }

    private static final class Fila {
        private String equipo;
        private int numero;
        private String jugador;
        private UUID idJornadaTarjeta;
        private int amarillas;
        private int rojas;
        private boolean pagada;
        private final Set<UUID> marcadas = new HashSet<>();
    }
}
