package com.away.core.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final AppUserRepository users;
    private final LeagueMembershipRepository memberships;
    private final UsuarioLeagueRepository leagues;
    private final PasswordEncoder passwords;

    public UsuarioService(AppUserRepository users,
                          LeagueMembershipRepository memberships,
                          UsuarioLeagueRepository leagues,
                          PasswordEncoder passwords) {
        this.users = users;
        this.memberships = memberships;
        this.leagues = leagues;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<UsuarioViews.UsuarioView> listar(UUID leagueId, String estado) {
        exigirMaster();
        League league = liga(leagueId);
        String filtro = estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT);
        return users.listarConMembresias().stream()
                .filter(user -> filtro.isEmpty() || filtro.equals(user.getEstado()))
                .map(user -> vista(user, league.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioViews.UsuarioView crear(UsuarioViews.UsuarioRequest request) {
        String actor = exigirMaster();
        League league = liga(request.getLeagueId());
        String username = usuario(request.getUsername());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese usuario ya existe");
        }
        String clave = request.getPassword() == null ? "" : request.getPassword().trim();
        if (clave.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave es obligatoria");
        }
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPasswordHash(passwords.encode(clave));
        user.setDisplayName(nombre(request.getDisplayName()));
        user.setMaster(false);
        user.setEstado(estado(request.getEstado(), false));
        aplicarFoto(user, request);
        OffsetDateTime ahora = OffsetDateTime.now();
        user.setFechaCreacion(ahora);
        user.setFechaModificacion(ahora);
        user.setUsuarioCreacion(actor);
        user.setUsuarioModificacion(actor);
        users.save(user);
        String role = rol(request.getRole());
        asignarRol(user, league, role);
        return new UsuarioViews.UsuarioView(user.getId(), user.getUsername(), user.getDisplayName(),
                user.isMaster(), user.getEstado(), role, user.getFoto() != null && user.getFoto().length > 0);
    }

    @Transactional
    public UsuarioViews.UsuarioView actualizar(UUID id, UsuarioViews.UsuarioRequest request) {
        String actor = exigirMaster();
        League league = liga(request.getLeagueId());
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
        user.setDisplayName(nombre(request.getDisplayName()));
        user.setEstado(estado(request.getEstado(), user.isMaster()));
        String clave = request.getPassword() == null ? "" : request.getPassword().trim();
        if (!user.isMaster() && !clave.isEmpty()) {
            if (clave.length() < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave debe tener al menos 4 caracteres");
            }
            user.setPasswordHash(passwords.encode(clave));
        }
        aplicarFoto(user, request);
        user.setFechaModificacion(OffsetDateTime.now());
        user.setUsuarioModificacion(actor);
        users.save(user);
        String role = null;
        if (!user.isMaster()) {
            role = rol(request.getRole());
            asignarRol(user, league, role);
        }
        return new UsuarioViews.UsuarioView(user.getId(), user.getUsername(), user.getDisplayName(),
                user.isMaster(), user.getEstado(), role, user.getFoto() != null && user.getFoto().length > 0);
    }

    @Transactional(readOnly = true)
    public AppUser foto(UUID id) {
        exigirMaster();
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
    }

    private void asignarRol(AppUser user, League league, String role) {
        LeagueMembership membership = memberships.findByUser_IdAndLeague_Id(user.getId(), league.getId())
                .orElseGet(LeagueMembership::new);
        if (membership.getId() == null) {
            membership.setId(UUID.randomUUID());
            membership.setUser(user);
            membership.setLeague(league);
        }
        membership.setRole(role);
        memberships.save(membership);
    }

    private UsuarioViews.UsuarioView vista(AppUser user, UUID leagueId) {
        String role = user.getMemberships().stream()
                .filter(item -> item.getLeague() != null && leagueId.equals(item.getLeague().getId()))
                .map(LeagueMembership::getRole)
                .findFirst()
                .orElse(null);
        return new UsuarioViews.UsuarioView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.isMaster(),
                user.getEstado(),
                role,
                user.getFoto() != null && user.getFoto().length > 0);
    }

    private void aplicarFoto(AppUser user, UsuarioViews.UsuarioRequest request) {
        if (request.isClearFoto()) {
            user.setFoto(null);
            user.setFotoContentType(null);
            return;
        }
        if (request.getFotoBase64() == null || request.getFotoBase64().trim().isEmpty()) {
            return;
        }
        user.setFoto(leerFoto(request.getFotoBase64(), request.getFotoContentType()));
        user.setFotoContentType(tipoImagen(request.getFotoContentType(), request.getFotoBase64()));
    }

    private String exigirMaster() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser actor = users.findWithMembershipsByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!actor.isMaster()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el master administra usuarios");
        }
        return actor.getUsername();
    }

    private League liga(UUID leagueId) {
        if (leagueId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una liga");
        }
        return leagues.findById(leagueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no existe"));
    }

    private String usuario(String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty() || texto.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario es obligatorio");
        }
        return texto;
    }

    private String nombre(String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.isEmpty() || texto.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        return texto;
    }

    private String rol(String valor) {
        String texto = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!"ADMINISTRATIVO".equals(texto) && !"ASISTENTE".equals(texto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }
        return texto;
    }

    private String estado(String valor, boolean master) {
        String texto = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!"ACTIVO".equals(texto) && !"INACTIVO".equals(texto) && !"BLOQUEADO".equals(texto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser activo, inactivo o bloqueado");
        }
        if (master && !"ACTIVO".equals(texto)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario master debe permanecer activo");
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
        if ("image/jpg".equals(type) || "image/pjpeg".equals(type)) {
            type = "image/jpeg";
        }
        if (encoded != null && encoded.startsWith("data:")) {
            int semi = encoded.indexOf(';');
            if (semi > 5) {
                type = encoded.substring(5, semi).trim().toLowerCase(Locale.ROOT);
            }
        }
        return type;
    }
}
