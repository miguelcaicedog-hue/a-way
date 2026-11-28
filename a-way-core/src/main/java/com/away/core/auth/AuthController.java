package com.away.core.auth;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository users;

    public AuthController(AuthenticationManager authenticationManager, AppUserRepository users) {
        this.authenticationManager = authenticationManager;
        this.users = users;
    }

    @PostMapping("/login")
    public UserProfile login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);
        return profile(authentication.getName());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> badCredentials(Exception exception) {
        if (exception instanceof DisabledException) {
            return Collections.singletonMap("error", "El usuario está inactivo o bloqueado");
        }
        return Collections.singletonMap("error", "Usuario o clave incorrectos");
    }

    @GetMapping("/me")
    public UserProfile me(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return profile(principal.getName());
    }

    @GetMapping("/foto")
    public ResponseEntity<byte[]> foto(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        AppUser user = users.findWithMembershipsByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getFoto() == null || user.getFotoContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(user.getFotoContentType()))
                .body(user.getFoto());
    }

    private UserProfile profile(String username) {
        AppUser user = users.findWithMembershipsByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        List<MembershipView> memberships = user.getMemberships().stream()
                .map(membership -> new MembershipView(
                        membership.getLeague().getId().toString(),
                        membership.getLeague().getName(),
                        membership.getRole()))
                .collect(Collectors.toList());
        return new UserProfile(user.getUsername(), user.getDisplayName(), user.isMaster(),
                user.getFoto() != null && user.getFoto().length > 0, memberships);
    }

    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class UserProfile {
        private final String username;
        private final String displayName;
        private final boolean master;
        private final boolean hasFoto;
        private final List<MembershipView> memberships;

        public UserProfile(String username, String displayName, boolean master, boolean hasFoto, List<MembershipView> memberships) {
            this.username = username;
            this.displayName = displayName;
            this.master = master;
            this.hasFoto = hasFoto;
            this.memberships = memberships;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isMaster() {
            return master;
        }

        public boolean isHasFoto() {
            return hasFoto;
        }

        public List<MembershipView> getMemberships() {
            return memberships;
        }
    }

    public static class MembershipView {
        private final String leagueId;
        private final String leagueName;
        private final String role;

        public MembershipView(String leagueId, String leagueName, String role) {
            this.leagueId = leagueId;
            this.leagueName = leagueName;
            this.role = role;
        }

        public String getLeagueId() {
            return leagueId;
        }

        public String getLeagueName() {
            return leagueName;
        }

        public String getRole() {
            return role;
        }
    }
}
