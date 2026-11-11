package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CanchaService {

    private final CanchaRepository canchas;
    private final LeagueRepository leagues;
    private final AppUserRepository users;

    public CanchaService(CanchaRepository canchas, LeagueRepository leagues, AppUserRepository users) {
        this.canchas = canchas;
        this.leagues = leagues;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CanchaViews.CanchaView> listar(UUID leagueId, String descripcion, String estado) {
        League league = league(leagueId);
        assertRead(league.getId());
        String texto = descripcion == null || descripcion.trim().isEmpty()
                ? ""
                : "%" + descripcion.trim().toLowerCase(Locale.ROOT) + "%";
        String filtroEstado = estado == null ? "" : estado.trim();
        return canchas.buscar(league.getId(), texto, filtroEstado).stream()
                .map(this::vista)
                .collect(Collectors.toList());
    }

    @Transactional
    public CanchaViews.CanchaView crear(CanchaViews.CanchaRequest request) {
        League league = league(request.getLeagueId());
        assertWrite(league.getId());
        String descripcion = descripcion(request.getDescripcion());
        String estado = estado(request.getEstado());
        asegurarUnica(league.getId(), descripcion, null);
        Cancha cancha = new Cancha();
        cancha.setId(UUID.randomUUID());
        cancha.setIdLiga(league.getId());
        cancha.setDescripcion(descripcion);
        cancha.setEstado(estado);
        auditar(cancha, usuario(), true);
        return vista(canchas.save(cancha));
    }

    @Transactional
    public CanchaViews.CanchaView actualizar(UUID id, CanchaViews.CanchaRequest request) {
        Cancha cancha = cancha(id);
        assertWrite(cancha.getIdLiga());
        if (!cancha.getIdLiga().equals(request.getLeagueId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cancha no pertenece a esta liga");
        }
        if ("ELIMINADO".equals(cancha.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cancha ya no está activa");
        }
        String descripcion = descripcion(request.getDescripcion());
        asegurarUnica(cancha.getIdLiga(), descripcion, cancha.getId());
        cancha.setDescripcion(descripcion);
        cancha.setEstado(estado(request.getEstado()));
        auditar(cancha, usuario(), false);
        return vista(canchas.save(cancha));
    }

    @Transactional
    public void eliminar(UUID id) {
        Cancha cancha = cancha(id);
        assertWrite(cancha.getIdLiga());
        if ("ELIMINADO".equals(cancha.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cancha ya no está activa");
        }
        cancha.setEstado("ELIMINADO");
        auditar(cancha, usuario(), false);
    }

    private void asegurarUnica(UUID leagueId, String descripcion, UUID excepto) {
        boolean repetida = canchas.buscar(leagueId, "", "").stream()
                .anyMatch(cancha -> (excepto == null || !excepto.equals(cancha.getId()))
                        && cancha.getDescripcion().equalsIgnoreCase(descripcion));
        if (repetida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una cancha con esa descripción");
        }
    }

    private String descripcion(String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria");
        }
        return texto;
    }

    private String estado(String valor) {
        if ("ACTIVO".equals(valor) || "INACTIVO".equals(valor)) {
            return valor;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser activo o inactivo");
    }

    private CanchaViews.CanchaView vista(Cancha cancha) {
        return new CanchaViews.CanchaView(cancha.getId(), cancha.getIdLiga(), cancha.getDescripcion(), cancha.getEstado());
    }

    private Cancha cancha(UUID id) {
        return canchas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cancha no existe"));
    }

    private League league(UUID id) {
        return leagues.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no existe"));
    }

    private void auditar(Cancha cancha, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            cancha.setFechaCreacion(ahora);
            cancha.setUsuarioCreacion(usuario);
        }
        cancha.setFechaModificacion(ahora);
        cancha.setUsuarioModificacion(usuario);
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
