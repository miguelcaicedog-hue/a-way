package com.away.core.competition;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService horarios;

    public HorarioController(HorarioService horarios) {
        this.horarios = horarios;
    }

    @GetMapping
    public HorarioViews.HorarioPanelView panel(@RequestParam UUID editionId,
                                                @RequestParam(required = false) UUID categoryId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                @RequestParam(required = false) List<Integer> jornadas) {
        return horarios.panel(editionId, categoryId, fecha, jornadas, null);
    }

    @PutMapping("/partido/{id}")
    public HorarioViews.HorarioPanelView guardar(@PathVariable UUID id,
                                                  @RequestBody HorarioViews.HorarioGuardarRequest request) {
        return horarios.guardar(id, request);
    }

    @PostMapping("/dia")
    public HorarioViews.HorarioPanelView asignarDia(@RequestBody HorarioViews.HorarioGuardarRequest request) {
        return horarios.asignarDia(request);
    }

    @PostMapping("/cancha")
    public HorarioViews.HorarioPanelView quitarCancha(@RequestBody HorarioViews.HorarioGuardarRequest request) {
        return horarios.quitarCancha(request);
    }
}
