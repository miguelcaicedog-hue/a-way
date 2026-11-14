package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
            cuerpo.append(htmlBloque(categoria.getName(), fecha, bloque));
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
            boolean roja = tarjeta.getRojas() != null && tarjeta.getRojas() > 0;
            fila.color = roja ? colores.roja : colores.amarilla;
            fila.pagada = pagadas.contains(tarjeta.getId());
            fila.marcadas.addAll(jornadasSuspendidas(activas, tarjeta.getIdJugador(), orden, indice));
            fila.marcadas.remove(fila.idJornadaTarjeta);
            filas.add(fila);
        }
        filas.sort(Comparator.comparing((Fila fila) -> fila.equipo).thenComparing(fila -> fila.jugador));
        Bloque bloque = new Bloque();
        bloque.filas = filas;
        bloque.columnas = orden;
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

    private String htmlBloque(String categoria, LocalDate fecha, Bloque bloque) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>").append(esc(categoria)).append(" · ").append(fecha.format(DIA)).append("</h2>");
        html.append("<table><thead><tr><th></th><th></th><th></th>");
        int indice = 0;
        while (indice < bloque.columnas.size()) {
            LocalDate dia = bloque.columnas.get(indice).getFecha();
            int ancho = 1;
            while (indice + ancho < bloque.columnas.size()
                    && mismoDia(dia, bloque.columnas.get(indice + ancho).getFecha())) {
                ancho++;
            }
            html.append("<th class=\"fecha\" colspan=\"").append(ancho).append("\">")
                    .append(dia == null ? "" : fechaCorta(dia)).append("</th>");
            indice += ancho;
        }
        html.append("</tr><tr><th>Equipo</th><th>Nº</th><th>Jugador</th>");
        for (Jornada jornada : bloque.columnas) {
            html.append("<th class=\"jornada\">").append(vertical(jornada.getNombre())).append("</th>");
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
                    String fondo = cssColor(fila.color);
                    html.append("<td class=\"cuadro\" style=\"background:").append(fondo)
                            .append(";color:").append(tinta(fondo)).append(";\">")
                            .append(fila.pagada ? "P" : "").append("</td>");
                    columna++;
                    continue;
                }
                if (fila.marcadas.contains(jornada.getId())) {
                    int ancho = 1;
                    while (columna + ancho < bloque.columnas.size()
                            && fila.marcadas.contains(bloque.columnas.get(columna + ancho).getId())) {
                        ancho++;
                    }
                    html.append("<td class=\"cuadro susp\" colspan=\"").append(ancho).append("\">SUSP</td>");
                    columna += ancho;
                    continue;
                }
                html.append("<td class=\"cuadro\"></td>");
                columna++;
            }
            html.append("</tr>");
            }
            filaIndice += grupo;
        }
        html.append("</tbody></table>");
        return html.toString();
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
        String limpio = valor.toLowerCase(java.util.Locale.forLanguageTag("es"));
        return limpio.substring(0, 1).toUpperCase(java.util.Locale.forLanguageTag("es")) + limpio.substring(1);
    }

    private String vertical(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            if (i > 0) {
                texto.append("<br/>");
            }
            char letra = valor.charAt(i);
            texto.append(letra == ' ' ? "&nbsp;" : esc(String.valueOf(letra)));
        }
        return texto.toString();
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
                + "h2 { font-size: 12px; margin: 14px 0 6px; }"
                + "table { width: 100%; border-collapse: collapse; }"
                + "th, td { border: 1px solid #163a5f; padding: 3px 4px; }"
                + "th { background: #163a5f; color: white; }"
                + "th.fecha { font-size: 7px; padding: 2px; text-align: center; }"
                + "th.jornada { font-size: 7px; line-height: 1.05; padding: 2px 1px; text-align: center; vertical-align: bottom; width: 14px; }"
                + "td.numero { text-align: center; white-space: nowrap; }"
                + "th.fecha { background: #2f6f9f; }"
                + "td.equipo, td.jugador { white-space: nowrap; }"
                + "td.cuadro { text-align: center; font-weight: 700; font-size: 8px; }"
                + "td.susp { background: #f3c98b; color: #163a5f; }"
                + "</style></head><body><div class=\"cabeza\">" + logo
                + "<h1>" + esc(liga.getName()) + "</h1>"
                + "<p class=\"sub\">DETALLE DE TARJETAS Y SUSPENSIONES</p></div>"
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
        private String color;
        private boolean pagada;
        private final Set<UUID> marcadas = new HashSet<>();
    }
}
