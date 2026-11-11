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
@RequestMapping("/api/fases")
public class FaseController {

    private final FaseService fases;

    public FaseController(FaseService fases) {
        this.fases = fases;
    }

    @GetMapping
    public FasePanelView panel(@RequestParam UUID categoryId) {
        return fases.panel(categoryId);
    }

    @PostMapping("/generar")
    public FasePanelView generar(@Valid @RequestBody FaseCategoriaRequest request) {
        return fases.generar(request.getCategoryId());
    }

    @PutMapping("/{id}/modalidad")
    public FasePanelView modalidad(@PathVariable UUID id, @Valid @RequestBody FaseModalidadRequest request) {
        return fases.modalidad(id, request.getModalidad());
    }

    @PutMapping("/cruce")
    public FasePanelView cruce(@Valid @RequestBody FaseCruceRequest request) {
        return fases.asignar(request.getCruceId(), request.getLado(), request.getGroupId(), request.getPosicion());
    }

    @DeleteMapping
    public FasePanelView eliminar(@RequestParam UUID categoryId) {
        return fases.eliminar(categoryId);
    }
}
