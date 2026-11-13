package com.away.core.competition;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/suspensiones-jugador")
public class SuspensionesJugadorController {

    private final SuspensionesJugadorService suspensiones;

    public SuspensionesJugadorController(SuspensionesJugadorService suspensiones) {
        this.suspensiones = suspensiones;
    }

    @GetMapping
    public SuspensionesJugadorViews.PanelView listar(@RequestParam UUID editionId,
                                                     @RequestParam UUID categoryId,
                                                     @RequestParam UUID teamId,
                                                     @RequestParam(required = false) String alias,
                                                     @RequestParam(required = false) Integer numero) {
        return suspensiones.listar(editionId, categoryId, teamId, alias, numero);
    }

    @GetMapping("/jugador/{id}")
    public SuspensionesJugadorViews.DetalleView detalle(@PathVariable UUID id, @RequestParam UUID categoryId) {
        return suspensiones.detalle(id, categoryId);
    }

    @PostMapping
    public SuspensionesJugadorViews.DetalleView agregar(@RequestBody SuspensionesJugadorViews.AgregarRequest request) {
        return suspensiones.agregar(request);
    }

    @DeleteMapping("/{id}")
    public void quitar(@PathVariable UUID id) {
        suspensiones.quitar(id);
    }
}
