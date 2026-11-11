package com.away.core.competition;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/grupos")
public class GrupoController {

    private final GrupoService grupos;

    public GrupoController(GrupoService grupos) {
        this.grupos = grupos;
    }

    @GetMapping
    public GrupoPanelView panel(@RequestParam UUID categoryId) {
        return grupos.panel(categoryId);
    }

    @PostMapping("/generar")
    public GrupoPanelView generar(@Valid @RequestBody GrupoCategoriaRequest request) {
        return grupos.generar(request.getCategoryId());
    }

    @PostMapping
    public GrupoPanelView agregar(@Valid @RequestBody GrupoCategoriaRequest request) {
        return grupos.agregar(request.getCategoryId());
    }

    @DeleteMapping("/{id}")
    public GrupoPanelView eliminar(@PathVariable UUID id) {
        return grupos.eliminar(id);
    }

    @PostMapping("/asignar")
    public GrupoPanelView asignar(@Valid @RequestBody GrupoCategoriaRequest request) {
        return grupos.asignarAutomatico(request.getCategoryId());
    }

    @PutMapping("/equipo")
    public GrupoPanelView mover(@Valid @RequestBody GrupoMoverRequest request) {
        return grupos.mover(request.getCategoryId(), request.getTeamId(), request.getGroupId());
    }
}
