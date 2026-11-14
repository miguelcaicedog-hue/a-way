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
public class TarjetaService {

    private final TarjetaRepository tarjetas;
    private final LeagueRepository leagues;
    private final AppUserRepository users;

    public TarjetaService(TarjetaRepository tarjetas, LeagueRepository leagues, AppUserRepository users) {
        this.tarjetas = tarjetas;
        this.leagues = leagues;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<TarjetaViews.TarjetaView> listar(UUID leagueId, String descripcion, String estado) {
        League league = league(leagueId);
        assertRead(league.getId());
        return tarjetas.buscar(league.getId(), texto(descripcion), texto(estado)).stream()
                .map(this::vista)
                .collect(Collectors.toList());
    }

    @Transactional
    public void sembrar(UUID leagueId) {
        crearInicial(leagueId, "AMARILLA", "#FFFF00", new BigDecimal("1.00"), 0, 1);
        crearInicial(leagueId, "ROJA", "#FF0000", new BigDecimal("3.00"), 1, 3);
    }

    @Transactional
    public TarjetaViews.TarjetaView crear(TarjetaViews.TarjetaRequest request) {
        League league = league(request.getLeagueId());
        assertWrite(league.getId());
        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setId(UUID.randomUUID());
        tarjeta.setIdLiga(league.getId());
        aplicar(tarjeta, request);
        auditar(tarjeta, usuario(), true);
        return vista(tarjetas.save(tarjeta));
    }

    @Transactional
    public TarjetaViews.TarjetaView actualizar(UUID id, TarjetaViews.TarjetaRequest request) {
        Tarjeta tarjeta = tarjeta(id);
        assertWrite(tarjeta.getIdLiga());
        if (!tarjeta.getIdLiga().equals(request.getLeagueId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La tarjeta no pertenece a esta liga");
        }
        aplicar(tarjeta, request);
        auditar(tarjeta, usuario(), false);
        return vista(tarjetas.save(tarjeta));
    }

    @Transactional
    public void eliminar(UUID id) {
        Tarjeta tarjeta = tarjeta(id);
        assertWrite(tarjeta.getIdLiga());
        tarjeta.setEstado("INACTIVO");
        auditar(tarjeta, usuario(), false);
    }

    private void crearInicial(UUID leagueId, String descripcion, String color, BigDecimal costo, int jornadas, int peso) {
        Tarjeta tarjeta = new Tarjeta();
        tarjeta.setId(UUID.randomUUID());
        tarjeta.setIdLiga(leagueId);
        tarjeta.setDescripcion(descripcion);
        tarjeta.setColor(color);
        tarjeta.setCosto(costo);
        tarjeta.setCantidadJornadas(jornadas);
        tarjeta.setPesoFairPlay(peso);
        tarjeta.setEstado("ACTIVO");
        auditar(tarjeta, usuario(), true);
        tarjetas.save(tarjeta);
    }

    private void aplicar(Tarjeta tarjeta, TarjetaViews.TarjetaRequest request) {
        String descripcion = descripcion(request.getDescripcion());
        String estado = estado(request.getEstado());
        if ("ACTIVO".equals(estado)) {
            asegurarUnica(tarjeta.getIdLiga(), descripcion, tarjeta.getId());
        }
        tarjeta.setDescripcion(descripcion);
        tarjeta.setColor(color(request.getColor()));
        tarjeta.setCosto(costo(request.getCosto()));
        tarjeta.setCantidadJornadas(jornadas(request.getCantidadJornadas()));
        tarjeta.setPesoFairPlay(peso(request.getPesoFairPlay()));
        tarjeta.setEstado(estado);
    }

    private void asegurarUnica(UUID leagueId, String descripcion, UUID excepto) {
        boolean repetida = tarjetas.buscar(leagueId, "", "ACTIVO").stream()
                .anyMatch(tarjeta -> tarjeta.getDescripcion().equalsIgnoreCase(descripcion)
                        && (excepto == null || !excepto.equals(tarjeta.getId())));
        if (repetida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe una tarjeta activa con esa descripción");
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

    private String color(String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (!texto.matches("#[0-9A-Fa-f]{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige un color");
        }
        return texto.toUpperCase(Locale.ROOT);
    }

    private BigDecimal costo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0 || valor.scale() > 2 && valor.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El costo debe ser cero o mayor, con hasta dos decimales");
        }
        return valor.setScale(2);
    }

    private int jornadas(Integer valor) {
        if (valor == null || valor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las jornadas de suspensión deben ser cero o más");
        }
        return valor;
    }

    private int peso(Integer valor) {
        if (valor == null) {
            return 1;
        }
        if (valor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El peso de Fair Play debe ser cero o más");
        }
        return valor;
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

    private TarjetaViews.TarjetaView vista(Tarjeta tarjeta) {
        return new TarjetaViews.TarjetaView(
                tarjeta.getId(),
                tarjeta.getIdLiga(),
                tarjeta.getDescripcion(),
                tarjeta.getColor(),
                tarjeta.getCosto(),
                tarjeta.getCantidadJornadas(),
                tarjeta.getPesoFairPlay(),
                tarjeta.getEstado());
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private Tarjeta tarjeta(UUID id) {
        return tarjetas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La tarjeta no existe"));
    }

    private League league(UUID id) {
        return leagues.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no existe"));
    }

    private void auditar(Tarjeta tarjeta, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            tarjeta.setFechaCreacion(ahora);
            tarjeta.setUsuarioCreacion(usuario);
        }
        tarjeta.setFechaModificacion(ahora);
        tarjeta.setUsuarioModificacion(usuario);
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
