package com.away.core.competition;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/goleadores")
public class GoleadorController {

    private final GoleadorService goleadores;

    public GoleadorController(GoleadorService goleadores) {
        this.goleadores = goleadores;
    }

    @GetMapping
    public GoleadorViews.PanelView listar(@RequestParam UUID editionId,
                                          @RequestParam UUID categoryId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                          @RequestParam(required = false) UUID teamId) {
        return goleadores.listar(editionId, categoryId, fecha, teamId);
    }
}
