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
    private final PartidoCobroDetalleRepository cobros;
    private final SuspensionJugadorRepository suspensiones;
    private final PlayerRepository jugadores;
    private final AppUserRepository users;

    public ReporteTarjetasService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                                  PartidoRepository partidos, TarjetaJugadorRepository tarjetas,
                                  PartidoCobroDetalleRepository cobros, SuspensionJugadorRepository suspensiones,
                                  PlayerRepository jugadores, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.tarjetas = tarjetas;
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
        StringBuilder cuerpo = new StringBuilder();
        boolean alguna = false;
        for (Category categoria : elegidas) {
            Bloque bloque = bloque(categoria, fecha, pagadas);
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

    private Bloque bloque(Category categoria, LocalDate fecha, Set<UUID> pagadas) {
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
            Jornada jornada = porJornada.get(partido.getIdJornada());
            Fila fila = new Fila();
            fila.equipo = jugador.getTeamEntry() == null || jugador.getTeamEntry().getTeam() == null
                    ? "" : jugador.getTeamEntry().getTeam().getName();
            fila.jugador = jugador.getAlias() == null ? "" : jugador.getAlias();
            fila.numero = jugador.getNumeroCamisa();
            fila.jornada = jornada == null ? "" : jornada.getNombre();
            fila.tarjeta = textoTarjeta(tarjeta);
            fila.pagada = pagadas.contains(tarjeta.getId());
            fila.marcadas.addAll(jornadasSuspendidas(activas, tarjeta.getIdJugador(), orden, indice));
            filas.add(fila);
        }
        filas.sort(Comparator.comparing((Fila fila) -> fila.equipo).thenComparing(fila -> fila.jugador));
        List<Jornada> columnas = new ArrayList<>();
        Set<UUID> vistas = new HashSet<>();
        for (Fila fila : filas) {
            for (Jornada jornada : orden) {
                if (fila.marcadas.contains(jornada.getId()) && vistas.add(jornada.getId())) {
                    columnas.add(jornada);
                }
            }
        }
        Bloque bloque = new Bloque();
        bloque.filas = filas;
        bloque.columnas = columnas;
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
        html.append("<table><thead><tr><th>Equipo</th><th>Nº</th><th>Jugador</th><th>Jornada</th><th>Tarjeta</th><th>Pagada</th>");
        for (Jornada jornada : bloque.columnas) {
            String dia = jornada.getFecha() == null ? "" : jornada.getFecha().format(DIA);
            html.append("<th>").append(esc(jornada.getNombre())).append("<br/>").append(esc(dia)).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        for (Fila fila : bloque.filas) {
            html.append("<tr><td>").append(esc(fila.equipo)).append("</td><td>").append(fila.numero)
                    .append("</td><td>").append(esc(fila.jugador)).append("</td><td>").append(esc(fila.jornada))
                    .append("</td><td>").append(esc(fila.tarjeta)).append("</td><td>")
                    .append(fila.pagada ? "SI" : "NO").append("</td>");
            for (Jornada jornada : bloque.columnas) {
                html.append("<td class=\"marca\">").append(fila.marcadas.contains(jornada.getId()) ? "X" : "").append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String documento(League liga, String cuerpo) {
        String logo = "";
        if (liga.getLogo() != null && liga.getLogo().length > 0) {
            String tipo = liga.getLogoContentType() == null ? "image/png" : liga.getLogoContentType();
            logo = "<img class=\"logo\" src=\"data:" + tipo + ";base64,"
                    + Base64.getEncoder().encodeToString(liga.getLogo()) + "\" alt=\"\"/>";
        }
        return "<html><head><meta charset=\"utf-8\"/><style>"
                + "@page { size: A4 landscape; margin: 14mm 10mm 12mm; @bottom-center { content: counter(page); font-size: 9px; } }"
                + "body { font-family: sans-serif; font-size: 10px; color: #163a5f; }"
                + ".cabeza { text-align: center; margin-bottom: 12px; }"
                + ".logo { height: 58px; }"
                + "h1 { font-size: 16px; margin: 6px 0 0; }"
                + "p.sub { margin: 2px 0 0; font-weight: 700; }"
                + "h2 { font-size: 12px; margin: 14px 0 6px; }"
                + "table { width: 100%; border-collapse: collapse; }"
                + "th, td { border: 1px solid #163a5f; padding: 3px 4px; }"
                + "th { background: #163a5f; color: white; }"
                + "td.marca { text-align: center; font-weight: 700; }"
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

    private String textoTarjeta(TarjetaJugador tarjeta) {
        List<String> partes = new ArrayList<>();
        if (tarjeta.getAmarillas() != null && tarjeta.getAmarillas() > 0) {
            partes.add(tarjeta.getAmarillas() + (tarjeta.getAmarillas() == 1 ? " amarilla" : " amarillas"));
        }
        if (tarjeta.getRojas() != null && tarjeta.getRojas() > 0) {
            partes.add(tarjeta.getRojas() + (tarjeta.getRojas() == 1 ? " roja" : " rojas"));
        }
        return String.join(", ", partes);
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

    private static final class Fila {
        private String equipo;
        private int numero;
        private String jugador;
        private String jornada;
        private String tarjeta;
        private boolean pagada;
        private final Set<UUID> marcadas = new HashSet<>();
    }
}
