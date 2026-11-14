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
@RequestMapping("/api/criterios-desempate-goleador")
public class CriterioDesempateGoleadorController {

    private final CriterioDesempateGoleadorService criterios;

    public CriterioDesempateGoleadorController(CriterioDesempateGoleadorService criterios) {
        this.criterios = criterios;
    }

    @GetMapping
    public List<CriterioDesempateGoleadorService.CriterioView> listar(@RequestParam UUID editionId) {
        return criterios.listar(editionId);
    }

    @PutMapping
    public List<CriterioDesempateGoleadorService.CriterioView> reordenar(
            @RequestBody CriterioDesempateGoleadorService.OrdenRequest request) {
        return criterios.reordenar(request.getEditionId(), request.getCodigos());
    }
}
