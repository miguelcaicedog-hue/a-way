package com.away.core.competition;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/suspensiones")
public class SuspensionesController {

    private final SuspensionesService suspensiones;

    public SuspensionesController(SuspensionesService suspensiones) {
        this.suspensiones = suspensiones;
    }

    @GetMapping
    public SuspensionesViews.ReglaView ver(@RequestParam UUID leagueId) {
        return suspensiones.ver(leagueId);
    }

    @PutMapping
    public SuspensionesViews.ReglaView guardar(@Valid @RequestBody SuspensionesViews.GuardarRequest request) {
        return suspensiones.guardar(request);
    }
}
