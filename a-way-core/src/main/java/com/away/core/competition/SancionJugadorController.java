package com.away.core.competition;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sanciones-jugador")
public class SancionJugadorController {

    private final SancionJugadorService sanciones;

    public SancionJugadorController(SancionJugadorService sanciones) {
        this.sanciones = sanciones;
    }

    @GetMapping
    public List<SancionJugadorViews.SancionJugadorView> listar(@RequestParam UUID leagueId,
                                                               @RequestParam(required = false) String descripcion,
                                                               @RequestParam(required = false) String estado) {
        return sanciones.listar(leagueId, descripcion, estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SancionJugadorViews.SancionJugadorView crear(@Valid @RequestBody SancionJugadorViews.SancionJugadorRequest request) {
        return sanciones.crear(request);
    }

    @PutMapping("/{id}")
    public SancionJugadorViews.SancionJugadorView actualizar(@PathVariable UUID id,
                                                             @Valid @RequestBody SancionJugadorViews.SancionJugadorRequest request) {
        return sanciones.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        sanciones.eliminar(id);
    }
}
