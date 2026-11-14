package com.away.core.competition;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/formatos-publicacion")
public class FormatoPublicacionController {

    private final FormatoPublicacionService formatos;

    public FormatoPublicacionController(FormatoPublicacionService formatos) {
        this.formatos = formatos;
    }

    @GetMapping
    public FormatoPublicacionViews.FormatoView obtener(@RequestParam UUID leagueId) {
        return formatos.obtener(leagueId);
    }

    @PutMapping
    public FormatoPublicacionViews.FormatoView guardar(@RequestParam UUID leagueId,
                                                       @RequestBody FormatoPublicacionViews.FormatoRequest request) {
        return formatos.guardar(leagueId, request);
    }
}
