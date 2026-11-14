package com.away.core.competition;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/criterios-desempate-equipo")
public class CriterioDesempateEquipoController {

    private final CriterioDesempateEquipoService criterios;

    public CriterioDesempateEquipoController(CriterioDesempateEquipoService criterios) {
        this.criterios = criterios;
    }

    @GetMapping
    public List<CriterioDesempateEquipoService.CriterioView> listar(@RequestParam UUID editionId) {
        return criterios.listar(editionId);
    }

    @PutMapping
    public List<CriterioDesempateEquipoService.CriterioView> reordenar(
            @RequestBody CriterioDesempateEquipoService.OrdenRequest request) {
        return criterios.reordenar(request.getEditionId(), request.getCodigos());
    }
}
