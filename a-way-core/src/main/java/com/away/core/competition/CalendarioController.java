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
@RequestMapping("/api/calendario")
public class CalendarioController {

    private final CalendarioService calendario;

    public CalendarioController(CalendarioService calendario) {
        this.calendario = calendario;
    }

    @GetMapping
    public CalendarioPanelView panel(@RequestParam UUID categoryId) {
        return calendario.panel(categoryId);
    }

    @PostMapping("/generar")
    public CalendarioPanelView generar(@Valid @RequestBody CalendarioCategoriaRequest request) {
        return calendario.generar(request.getCategoryId());
    }

    @PutMapping("/jornada/{id}")
    public CalendarioPanelView fechaJornada(@PathVariable UUID id, @RequestBody CalendarioFechaRequest request) {
        return calendario.fechaJornada(id, request.getFecha());
    }

    @PutMapping("/partido/{id}")
    public CalendarioPanelView fechaPartido(@PathVariable UUID id, @RequestBody CalendarioFechaRequest request) {
        return calendario.fechaPartido(id, request.getFecha());
    }

    @DeleteMapping
    public CalendarioPanelView eliminar(@RequestParam UUID categoryId) {
        return calendario.eliminar(categoryId);
    }
}
