package com.away.core.competition;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/arbitros")
public class ArbitroController {

    private final ArbitroService arbitros;

    public ArbitroController(ArbitroService arbitros) {
        this.arbitros = arbitros;
    }

    @GetMapping
    public List<ArbitroViews.ArbitroView> listar(@RequestParam UUID leagueId,
                                                  @RequestParam(required = false) String cedula,
                                                  @RequestParam(required = false) String nombre,
                                                  @RequestParam(required = false) String estado) {
        return arbitros.listar(leagueId, cedula, nombre, estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArbitroViews.ArbitroView crear(@Valid @RequestBody ArbitroViews.ArbitroRequest request) {
        return arbitros.crear(request);
    }

    @PutMapping("/{id}")
    public ArbitroViews.ArbitroView actualizar(@PathVariable UUID id, @Valid @RequestBody ArbitroViews.ArbitroRequest request) {
        return arbitros.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        arbitros.eliminar(id);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable UUID id) {
        Arbitro arbitro = arbitros.foto(id);
        if (arbitro.getFoto() == null || arbitro.getFotoContentType() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(arbitro.getFotoContentType()))
                .body(arbitro.getFoto());
    }
}
