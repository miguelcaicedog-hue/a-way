package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CriterioDesempateEquipoService {

    static final List<String> ORDEN_INICIAL = Arrays.asList(
            "FAIR_PLAY", "GOLES_FAVOR", "ENFRENTAMIENTO_DIRECTO");

    private final CriterioDesempateEquipoRepository criterios;
    private final EditionRepository editions;
    private final AppUserRepository users;

    public CriterioDesempateEquipoService(CriterioDesempateEquipoRepository criterios, EditionRepository editions,
                                            AppUserRepository users) {
        this.criterios = criterios;
        this.editions = editions;
        this.users = users;
    }

    @Transactional
    public List<CriterioView> listar(UUID editionId) {
        Edition edition = edition(editionId);
        assertRead(edition.getLeague().getId());
        return vistas(asegurar(edition));
    }

    @Transactional
    public List<CriterioView> reordenar(UUID editionId, List<String> codigos) {
        Edition edition = edition(editionId);
        assertWrite(edition.getLeague().getId());
        List<String> pedido = normalizar(codigos);
        List<CriterioDesempateEquipo> filas = asegurar(edition);
        String usuario = usuario();
        for (CriterioDesempateEquipo fila : filas) {
            fila.setOrden(pedido.indexOf(fila.getCodigo()) + 1 + ORDEN_INICIAL.size());
            auditar(fila, usuario, false);
        }
        criterios.flush();
        for (CriterioDesempateEquipo fila : filas) {
            fila.setOrden(pedido.indexOf(fila.getCodigo()) + 1);
            auditar(fila, usuario, false);
        }
        filas.sort((a, b) -> Integer.compare(a.getOrden(), b.getOrden()));
        return vistas(filas);
    }

    @Transactional(readOnly = true)
    public List<String> orden(UUID editionId) {
        List<CriterioDesempateEquipo> filas = criterios.findByIdEdicionAndEstadoOrderByOrdenAsc(editionId, "ACTIVO");
        if (filas.isEmpty()) {
            return ORDEN_INICIAL;
        }
        List<String> codigos = new ArrayList<>();
        for (CriterioDesempateEquipo fila : filas) {
            codigos.add(fila.getCodigo());
        }
        return codigos;
    }

    public void sembrar(Edition edition) {
        asegurar(edition);
    }

    private List<CriterioDesempateEquipo> asegurar(Edition edition) {
        List<CriterioDesempateEquipo> filas = criterios.findByIdEdicionAndEstadoOrderByOrdenAsc(edition.getId(), "ACTIVO");
        if (!filas.isEmpty()) {
            return filas;
        }
        String usuario = usuario();
        List<CriterioDesempateEquipo> creados = new ArrayList<>();
        int orden = 1;
        for (String codigo : ORDEN_INICIAL) {
            CriterioDesempateEquipo fila = new CriterioDesempateEquipo();
            fila.setId(UUID.randomUUID());
            fila.setIdLiga(edition.getLeague().getId());
            fila.setIdEdicion(edition.getId());
            fila.setCodigo(codigo);
            fila.setOrden(orden++);
            fila.setEstado("ACTIVO");
            auditar(fila, usuario, true);
            creados.add(criterios.save(fila));
        }
        return creados;
    }

    private List<String> normalizar(List<String> codigos) {
        if (codigos == null || codigos.size() != ORDEN_INICIAL.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envía los tres criterios");
        }
        Set<String> unicos = new LinkedHashSet<>();
        for (String codigo : codigos) {
            String valor = codigo == null ? "" : codigo.trim().toUpperCase();
            if (!ORDEN_INICIAL.contains(valor) || !unicos.add(valor)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El orden debe incluir cada criterio una vez");
            }
        }
        return new ArrayList<>(unicos);
    }

    private List<CriterioView> vistas(List<CriterioDesempateEquipo> filas) {
        List<CriterioView> vistas = new ArrayList<>();
        for (CriterioDesempateEquipo fila : filas) {
            vistas.add(new CriterioView(fila.getCodigo(), fila.getOrden(), texto(fila.getCodigo())));
        }
        return vistas;
    }

    static String texto(String codigo) {
        if ("FAIR_PLAY".equals(codigo)) {
            return "Fair Play: prioridad al equipo con menor peso de tarjetas amarillas y rojas.";
        }
        if ("GOLES_FAVOR".equals(codigo)) {
            return "Goles a favor: mayor cantidad de goles anotados.";
        }
        return "Enfrentamiento directo: más puntos en los partidos contra los equipos empatados.";
    }

    private Edition edition(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una edición");
        }
        return editions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
    }

    private void auditar(CriterioDesempateEquipo fila, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            fila.setFechaCreacion(ahora);
            fila.setUsuarioCreacion(usuario);
        }
        fila.setFechaModificacion(ahora);
        fila.setUsuarioModificacion(usuario);
    }

    private String usuario() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private AppUser actual() {
        return users.findWithMembershipsByUsername(usuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void assertRead(UUID leagueId) {
        AppUser user = actual();
        boolean miembro = user.isMaster() || user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId));
        if (!miembro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta liga");
        }
    }

    private void assertWrite(UUID leagueId) {
        AppUser user = actual();
        if (user.isMaster()) {
            return;
        }
        boolean admin = user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId)
                        && "ADMINISTRATIVO".equals(membership.getRole()));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta liga");
        }
    }

    public static class CriterioView {
        private final String codigo;
        private final int orden;
        private final String descripcion;

        CriterioView(String codigo, int orden, String descripcion) {
            this.codigo = codigo;
            this.orden = orden;
            this.descripcion = descripcion;
        }

        public String getCodigo() {
            return codigo;
        }

        public int getOrden() {
            return orden;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public static class OrdenRequest {
        private UUID editionId;
        private List<String> codigos;

        public UUID getEditionId() {
            return editionId;
        }

        public void setEditionId(UUID editionId) {
            this.editionId = editionId;
        }

        public List<String> getCodigos() {
            return codigos;
        }

        public void setCodigos(List<String> codigos) {
            this.codigos = codigos;
        }
    }
}
