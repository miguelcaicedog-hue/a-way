package com.away.core.competition;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resultados")
public class ResultadoController {

    private final ResultadoService resultados;
    private final CobroPartidoService cobros;

    public ResultadoController(ResultadoService resultados, CobroPartidoService cobros) {
        this.resultados = resultados;
        this.cobros = cobros;
    }

    @GetMapping
    public ResultadoViews.ResultadoPanelView listar(@RequestParam UUID editionId,
                                                     @RequestParam(required = false) UUID categoryId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return resultados.listar(editionId, categoryId, fecha);
    }

    @GetMapping("/fecha-cercana")
    public ResultadoViews.ResultadoFechaView fechaCercana(@RequestParam UUID editionId,
                                                           @RequestParam(required = false) UUID categoryId) {
        return resultados.fechaCercana(editionId, categoryId);
    }

    @GetMapping("/fecha-anterior")
    public ResultadoViews.ResultadoFechaView fechaAnterior(@RequestParam UUID editionId,
                                                            @RequestParam(required = false) UUID categoryId,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return resultados.fechaAnterior(editionId, categoryId, desde);
    }

    @GetMapping("/proxima-fecha")
    public ResultadoViews.ResultadoFechaView proximaFecha(@RequestParam UUID editionId,
                                                           @RequestParam(required = false) UUID categoryId,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return resultados.proximaFecha(editionId, categoryId, desde);
    }

    @PutMapping("/jornada/cerrar")
    public ResultadoViews.CierreView cerrar(@RequestParam UUID editionId,
                                             @RequestParam UUID categoryId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return new ResultadoViews.CierreView(resultados.cerrarJornada(editionId, categoryId, fecha));
    }

    @PutMapping("/jornada/abrir")
    public ResultadoViews.CierreView abrir(@RequestParam UUID editionId,
                                            @RequestParam UUID categoryId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return new ResultadoViews.CierreView(resultados.abrirJornada(editionId, categoryId, fecha));
    }

    @PutMapping("/partido/{id}")
    public ResultadoViews.ResultadoPartidoView guardar(@PathVariable UUID id,
                                                        @RequestBody ResultadoViews.ResultadoGuardarRequest request) {
        return resultados.guardar(id, request);
    }

    @GetMapping("/partido/{id}/detalle")
    public ResultadoViews.DetalleView detalle(@PathVariable UUID id) {
        return resultados.detalle(id);
    }

    @GetMapping("/partido/{id}/jugadores")
    public List<ResultadoViews.JugadorPartidoView> jugadores(@PathVariable UUID id,
                                                              @RequestParam UUID teamId,
                                                              @RequestParam(required = false) String numero,
                                                              @RequestParam(required = false) String nombre) {
        return resultados.jugadores(id, teamId, numero, nombre);
    }

    @PutMapping("/partido/{id}/detalle")
    public ResultadoViews.DetalleView guardarDetalle(@PathVariable UUID id,
                                                      @RequestBody ResultadoViews.DetalleGuardarRequest request) {
        return resultados.guardarDetalle(id, request);
    }

    @GetMapping("/partido/{id}/cobro")
    public CobroViews.CobroView cobro(@PathVariable UUID id, @RequestParam(required = false) UUID teamId) {
        return cobros.ver(id, teamId);
    }

    @PutMapping("/partido/{id}/cobro")
    public CobroViews.CobroView guardarCobro(@PathVariable UUID id, @RequestBody CobroViews.CobroGuardarRequest request) {
        return cobros.guardar(id, request);
    }
}
