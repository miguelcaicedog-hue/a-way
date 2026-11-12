package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SancionJugadorService {

    private final SancionJugadorRepository sanciones;
    private final LeagueRepository leagues;
    private final AppUserRepository users;

    public SancionJugadorService(SancionJugadorRepository sanciones, LeagueRepository leagues, AppUserRepository users) {
        this.sanciones = sanciones;
        this.leagues = leagues;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<SancionJugadorViews.SancionJugadorView> listar(UUID leagueId, String descripcion, String estado) {
        League league = league(leagueId);
        assertRead(league.getId());
        return sanciones.buscar(league.getId(), texto(descripcion), texto(estado)).stream()
                .map(this::vista)
                .collect(Collectors.toList());
    }

    @Transactional
    public SancionJugadorViews.SancionJugadorView crear(SancionJugadorViews.SancionJugadorRequest request) {
        League league = league(request.getLeagueId());
        assertWrite(league.getId());
        SancionJugador sancion = new SancionJugador();
        sancion.setId(UUID.randomUUID());
        sancion.setIdLiga(league.getId());
        aplicar(sancion, request);
        auditar(sancion, usuario(), true);
        return vista(sanciones.save(sancion));
    }

    @Transactional
    public SancionJugadorViews.SancionJugadorView actualizar(UUID id, SancionJugadorViews.SancionJugadorRequest request) {
        SancionJugador sancion = sancion(id);
        assertWrite(sancion.getIdLiga());
        if (!sancion.getIdLiga().equals(request.getLeagueId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sanción no pertenece a esta liga");
        }
        aplicar(sancion, request);
        auditar(sancion, usuario(), false);
        return vista(sanciones.save(sancion));
    }

    @Transactional
    public void eliminar(UUID id) {
        SancionJugador sancion = sancion(id);
        assertWrite(sancion.getIdLiga());
        sancion.setEstado("INACTIVO");
        auditar(sancion, usuario(), false);
    }

    private void aplicar(SancionJugador sancion, SancionJugadorViews.SancionJugadorRequest request) {
        String descripcion = descripcion(request.getDescripcion());
        String estado = estado(request.getEstado());
        if ("ACTIVO".equals(estado)) {
            asegurarUnica(sancion.getIdLiga(), descripcion, sancion.getId());
        }
        sancion.setDescripcion(descripcion);
        sancion.setCosto(costo(request.getCosto()));
        sancion.setEstado(estado);
    }

    private void asegurarUnica(UUID leagueId, String descripcion, UUID excepto) {
        boolean repetida = sanciones.buscar(leagueId, "", "ACTIVO").stream()
                .anyMatch(sancion -> sancion.getDescripcion().equalsIgnoreCase(descripcion)
                        && (excepto == null || !excepto.equals(sancion.getId())));
        if (repetida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una sanción activa con esa descripción");
        }
    }

    private String descripcion(String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria");
        }
        if (texto.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es demasiado larga");
        }
        return texto;
    }

    private BigDecimal costo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.scale() > 2 && valor.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El costo debe ser cero o mayor, con hasta dos decimales");
        }
        return valor.setScale(2);
    }

    private String estado(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return "ACTIVO";
        }
        String texto = valor.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVO".equals(texto) && !"INACTIVO".equals(texto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser activo o inactivo");
        }
        return texto;
    }

    private SancionJugadorViews.SancionJugadorView vista(SancionJugador sancion) {
        return new SancionJugadorViews.SancionJugadorView(
                sancion.getId(),
                sancion.getIdLiga(),
                sancion.getDescripcion(),
                sancion.getCosto(),
                sancion.getEstado());
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private SancionJugador sancion(UUID id) {
        return sanciones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sanción no existe"));
    }

    private League league(UUID id) {
        return leagues.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no existe"));
    }

    private void auditar(SancionJugador sancion, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            sancion.setFechaCreacion(ahora);
            sancion.setUsuarioCreacion(usuario);
        }
        sancion.setFechaModificacion(ahora);
        sancion.setUsuarioModificacion(usuario);
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
}
