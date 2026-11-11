package com.away.core.competition;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/canchas")
public class CanchaController {

    private final CanchaService canchas;

    public CanchaController(CanchaService canchas) {
        this.canchas = canchas;
    }

    @GetMapping
    public List<CanchaViews.CanchaView> listar(@RequestParam UUID leagueId,
                                               @RequestParam(required = false) String descripcion,
                                               @RequestParam(required = false) String estado) {
        return canchas.listar(leagueId, descripcion, estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CanchaViews.CanchaView crear(@Valid @RequestBody CanchaViews.CanchaRequest request) {
        return canchas.crear(request);
    }

    @PutMapping("/{id}")
    public CanchaViews.CanchaView actualizar(@PathVariable UUID id, @Valid @RequestBody CanchaViews.CanchaRequest request) {
        return canchas.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        canchas.eliminar(id);
    }
}
