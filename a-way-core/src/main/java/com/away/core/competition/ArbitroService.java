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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArbitroService {

    private final ArbitroRepository arbitros;
    private final LeagueRepository leagues;
    private final AppUserRepository users;

    public ArbitroService(ArbitroRepository arbitros, LeagueRepository leagues, AppUserRepository users) {
        this.arbitros = arbitros;
        this.leagues = leagues;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ArbitroViews.ArbitroView> listar(UUID leagueId, String cedula, String nombre, String estado) {
        League league = league(leagueId);
        assertRead(league.getId());
        return arbitros.buscar(league.getId(), texto(cedula), texto(nombre), texto(estado)).stream()
                .map(this::vista)
                .collect(Collectors.toList());
    }

    @Transactional
    public ArbitroViews.ArbitroView crear(ArbitroViews.ArbitroRequest request) {
        League league = league(request.getLeagueId());
        assertWrite(league.getId());
        Arbitro arbitro = new Arbitro();
        arbitro.setId(UUID.randomUUID());
        arbitro.setIdLiga(league.getId());
        aplicar(arbitro, request, true);
        auditar(arbitro, usuario(), true);
        return vista(arbitros.save(arbitro));
    }

    @Transactional
    public ArbitroViews.ArbitroView actualizar(UUID id, ArbitroViews.ArbitroRequest request) {
        Arbitro arbitro = arbitro(id);
        assertWrite(arbitro.getIdLiga());
        if (!arbitro.getIdLiga().equals(request.getLeagueId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El árbitro no pertenece a esta liga");
        }
        aplicar(arbitro, request, false);
        auditar(arbitro, usuario(), false);
        return vista(arbitros.save(arbitro));
    }

    @Transactional
    public void eliminar(UUID id) {
        Arbitro arbitro = arbitro(id);
        assertWrite(arbitro.getIdLiga());
        if ("ELIMINADO".equals(arbitro.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El árbitro ya no está activo");
        }
        arbitro.setEstado("ELIMINADO");
        auditar(arbitro, usuario(), false);
    }

    @Transactional(readOnly = true)
    public Arbitro foto(UUID id) {
        Arbitro arbitro = arbitro(id);
        assertRead(arbitro.getIdLiga());
        arbitro.getFoto();
        return arbitro;
    }

    private void aplicar(Arbitro arbitro, ArbitroViews.ArbitroRequest request, boolean nuevo) {
        String cedula = opcional(request.getCedula(), 20);
        asegurarCedula(arbitro.getIdLiga(), cedula, arbitro.getId());
        arbitro.setCedula(cedula);
        arbitro.setNombre(nombre(request.getNombre()));
        arbitro.setTelefono(opcional(request.getTelefono(), 30));
        arbitro.setValorPagoPartido(valor(request.getValorPagoPartido(), "El valor de pago por partido"));
        arbitro.setValorCobroEquipo(valor(request.getValorCobroEquipo(), "El valor de cobro al equipo"));
        arbitro.setValorPagoSinPito(valor(request.getValorPagoSinPito(), "El valor de pago si no pita"));
        arbitro.setEstado(estado(request.getEstado()));
        if (request.isClearFoto()) {
            arbitro.getFoto();
            arbitro.setFoto(null);
            arbitro.setFotoContentType(null);
        }
        if (request.getFotoBase64() != null && !request.getFotoBase64().trim().isEmpty()) {
            byte[] foto = leerFoto(request.getFotoBase64(), request.getFotoContentType());
            arbitro.getFoto();
            arbitro.setFoto(foto);
            arbitro.setFotoContentType(tipoImagen(request.getFotoContentType(), request.getFotoBase64()));
        } else if (nuevo && !request.isClearFoto()) {
            arbitro.setFoto(null);
            arbitro.setFotoContentType(null);
        }
    }

    private void asegurarCedula(UUID leagueId, String cedula, UUID excepto) {
        if (cedula == null) {
            return;
        }
        boolean repetida = arbitros.buscar(leagueId, "", "", "").stream()
                .anyMatch(arbitro -> cedula.equalsIgnoreCase(arbitro.getCedula())
                        && !"ELIMINADO".equals(arbitro.getEstado())
                        && (excepto == null || !excepto.equals(arbitro.getId())));
        if (repetida) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un árbitro con esa cédula");
        }
    }

    private String nombre(String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (texto.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es demasiado largo");
        }
        return texto;
    }

    private BigDecimal valor(BigDecimal monto, String etiqueta) {
        if (monto == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (monto.signum() < 0 || monto.scale() > 2 && monto.stripTrailingZeros().scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, etiqueta + " debe ser cero o mayor, con hasta dos decimales");
        }
        return monto.setScale(2);
    }

    private String opcional(String valor, int maximo) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty()) {
            return null;
        }
        if (texto.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El dato es demasiado largo");
        }
        return texto;
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

    private byte[] leerFoto(String encoded, String declaredType) {
        String type = tipoImagen(declaredType, encoded);
        if (!Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe ser JPEG, PNG, WEBP o GIF");
        }
        int comma = encoded.indexOf(',');
        String payload = encoded.startsWith("data:") && comma > 0 ? encoded.substring(comma + 1) : encoded.trim();
        byte[] foto;
        try {
            foto = Base64.getDecoder().decode(payload.trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto no se pudo leer");
        }
        if (foto.length == 0 || foto.length > 1_500_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe pesar menos de 1.5 MB");
        }
        return foto;
    }

    private String tipoImagen(String declared, String encoded) {
        String type = declared == null ? "" : declared.trim().toLowerCase(Locale.ROOT);
        if (encoded != null && encoded.startsWith("data:")) {
            int semi = encoded.indexOf(';');
            if (semi > 5) {
                type = encoded.substring(5, semi).trim().toLowerCase(Locale.ROOT);
            }
        }
        if ("image/jpg".equals(type) || "image/pjpeg".equals(type)) {
            return "image/jpeg";
        }
        return type;
    }

    private ArbitroViews.ArbitroView vista(Arbitro arbitro) {
        return new ArbitroViews.ArbitroView(
                arbitro.getId(),
                arbitro.getIdLiga(),
                arbitro.getCedula(),
                arbitro.getNombre(),
                arbitro.getTelefono(),
                arbitro.getValorPagoPartido(),
                arbitro.getValorCobroEquipo(),
                arbitro.getValorPagoSinPito(),
                arbitro.getFoto() != null,
                arbitro.getEstado());
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private Arbitro arbitro(UUID id) {
        return arbitros.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El árbitro no existe"));
    }

    private League league(UUID id) {
        return leagues.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no existe"));
    }

    private void auditar(Arbitro arbitro, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            arbitro.setFechaCreacion(ahora);
            arbitro.setUsuarioCreacion(usuario);
        }
        arbitro.setFechaModificacion(ahora);
        arbitro.setUsuarioModificacion(usuario);
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
