package com.away.core.auth;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<UsuarioViews.UsuarioView> listar(@RequestParam UUID leagueId,
                                                  @RequestParam(required = false) String estado) {
        return usuarios.listar(leagueId, estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioViews.UsuarioView crear(@RequestBody UsuarioViews.UsuarioRequest request) {
        return usuarios.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioViews.UsuarioView actualizar(@PathVariable UUID id, @RequestBody UsuarioViews.UsuarioRequest request) {
        return usuarios.actualizar(id, request);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable UUID id) {
        AppUser user = usuarios.foto(id);
        if (user.getFoto() == null || user.getFotoContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(user.getFotoContentType()))
                .body(user.getFoto());
    }
}
