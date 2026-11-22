package com.away.core.competition;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/clasificacion")
public class ClasificacionController {

    private final ClasificacionService clasificacion;

    public ClasificacionController(ClasificacionService clasificacion) {
        this.clasificacion = clasificacion;
    }

    @GetMapping
    public ClasificacionViews.CuadroView cuadro(@RequestParam UUID editionId,
                                                @RequestParam UUID categoryId,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return clasificacion.cuadro(editionId, categoryId, fecha);
    }
}
