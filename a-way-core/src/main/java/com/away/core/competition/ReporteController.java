package com.away.core.competition;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteTarjetasService tarjetas;
    private final ReporteJugadoresService jugadores;

    public ReporteController(ReporteTarjetasService tarjetas, ReporteJugadoresService jugadores) {
        this.tarjetas = tarjetas;
        this.jugadores = jugadores;
    }

    @GetMapping(value = "/tarjetas-suspensiones", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> tarjetas(@RequestParam UUID editionId,
                                           @RequestParam(required = false) UUID categoryId,
                                           @RequestParam String fecha) {
        byte[] pdf = tarjetas.tarjetasYSuspensiones(editionId, categoryId, LocalDate.parse(fecha));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"amonestados-suspendidos.pdf\"")
                .body(pdf);
    }

    @GetMapping(value = "/jugadores-por-equipo", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> jugadores(@RequestParam UUID editionId, @RequestParam UUID categoryId) {
        byte[] pdf = jugadores.jugadoresPorEquipo(editionId, categoryId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jugadores-por-equipo.pdf\"")
                .body(pdf);
    }
}
