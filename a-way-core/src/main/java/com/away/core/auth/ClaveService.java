package com.away.core.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClaveService {

    private final PoliticaClaveRepository politicas;
    private final AppUserRepository users;
    private final PasswordEncoder passwords;

    public ClaveService(PoliticaClaveRepository politicas, AppUserRepository users, PasswordEncoder passwords) {
        this.politicas = politicas;
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public ClaveViews.PoliticaView politica() {
        return vista(vigente());
    }

    @Transactional
    public void cambiar(ClaveViews.CambioRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = users.findWithMembershipsByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String actual = request.getActual() == null ? "" : request.getActual();
        String nueva = request.getNueva() == null ? "" : request.getNueva();
        if (user.getPasswordHash() == null || !passwords.matches(actual, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La clave actual no es correcta");
        }
        List<String> faltan = faltantes(nueva, vigente());
        if (!faltan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", faltan));
        }
        user.setPasswordHash(passwords.encode(nueva));
        user.setFechaModificacion(OffsetDateTime.now());
        user.setUsuarioModificacion(username);
        users.save(user);
    }

    public List<String> faltantes(String clave, PoliticaClave politica) {
        String texto = clave == null ? "" : clave;
        List<String> faltan = new ArrayList<>();
        if (texto.length() < politica.getLongitudMinima()) {
            faltan.add("Debe tener al menos " + politica.getLongitudMinima() + " caracteres.");
        }
        if (politica.isRequiereLetra() && !texto.chars().anyMatch(Character::isLetter)) {
            faltan.add("Debe incluir al menos una letra.");
        }
        if (politica.isRequiereNumero() && !texto.chars().anyMatch(Character::isDigit)) {
            faltan.add("Debe incluir al menos un número.");
        }
        if (politica.isRequiereEspecial() && texto.chars().noneMatch(caracter -> !Character.isLetterOrDigit(caracter))) {
            faltan.add("Debe incluir al menos un caracter especial.");
        }
        return faltan;
    }

    private PoliticaClave vigente() {
        return politicas.findFirstByEstado("ACTIVO")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay una política de clave activa"));
    }

    private ClaveViews.PoliticaView vista(PoliticaClave politica) {
        return new ClaveViews.PoliticaView(
                politica.getLongitudMinima(),
                politica.isRequiereLetra(),
                politica.isRequiereNumero(),
                politica.isRequiereEspecial());
    }
}
