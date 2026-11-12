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
@RequestMapping("/api/tarjetas")
public class TarjetaController {

    private final TarjetaService tarjetas;

    public TarjetaController(TarjetaService tarjetas) {
        this.tarjetas = tarjetas;
    }

    @GetMapping
    public List<TarjetaViews.TarjetaView> listar(@RequestParam UUID leagueId,
                                                  @RequestParam(required = false) String descripcion,
                                                  @RequestParam(required = false) String estado) {
        return tarjetas.listar(leagueId, descripcion, estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarjetaViews.TarjetaView crear(@Valid @RequestBody TarjetaViews.TarjetaRequest request) {
        return tarjetas.crear(request);
    }

    @PutMapping("/{id}")
    public TarjetaViews.TarjetaView actualizar(@PathVariable UUID id, @Valid @RequestBody TarjetaViews.TarjetaRequest request) {
        return tarjetas.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        tarjetas.eliminar(id);
    }
}
