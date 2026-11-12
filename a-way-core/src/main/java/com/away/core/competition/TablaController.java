package com.away.core.competition;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/tabla")
public class TablaController {

    private final TablaService tabla;

    public TablaController(TablaService tabla) {
        this.tabla = tabla;
    }

    @GetMapping
    public TablaViews.TablaView listar(@RequestParam UUID editionId,
                                       @RequestParam UUID categoryId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return tabla.listar(editionId, categoryId, fecha);
    }
}
