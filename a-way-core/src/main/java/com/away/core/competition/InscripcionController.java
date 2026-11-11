package com.away.core.competition;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final InscripcionService inscripciones;

    public InscripcionController(InscripcionService inscripciones) {
        this.inscripciones = inscripciones;
    }

    @GetMapping
    public List<InscripcionArchivoView> listar(@RequestParam UUID leagueId, @RequestParam UUID editionId) {
        return inscripciones.listar(leagueId, editionId);
    }

    @GetMapping("/{id}")
    public InscripcionDetalleView detalle(@PathVariable UUID id) {
        return inscripciones.detalle(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InscripcionDetalleView cargar(@RequestParam UUID leagueId,
                                         @RequestParam UUID editionId,
                                         @RequestParam("archivo") MultipartFile archivo) {
        return inscripciones.cargar(leagueId, editionId, archivo);
    }

    @GetMapping("/equipos/{id}/jugadores")
    public List<InscripcionJugadorView> jugadores(@PathVariable UUID id) {
        return inscripciones.jugadores(id);
    }

    @PutMapping("/equipos/{id}/seleccion")
    public InscripcionEquipoView seleccionar(@PathVariable UUID id, @RequestBody SeleccionRequest request) {
        return inscripciones.seleccionar(id, request.isSeleccionado());
    }

    @GetMapping("/jugadores/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable UUID id) {
        InscripcionService.FotoContenido contenido = inscripciones.foto(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(contenido.tipo))
                .body(contenido.bytes);
    }

    @PutMapping(value = "/jugadores/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InscripcionJugadorView editarJugador(@PathVariable UUID id,
                                                @RequestParam String cedula,
                                                @RequestParam String nombre,
                                                @RequestParam(required = false) String alias,
                                                @RequestParam String numeroCamisa,
                                                @RequestParam(value = "foto", required = false) MultipartFile foto) {
        return inscripciones.editarJugador(id, cedula, nombre, alias, numeroCamisa, foto);
    }

    @PostMapping("/{id}/acceso-fotos")
    public AccesoFotosView accesoFotos(@PathVariable UUID id, @RequestBody ProcesoRequest request) {
        return inscripciones.accesoFotos(id, request.getEquipoIds());
    }

    @PostMapping("/{id}/procesar")
    public InscripcionDetalleView procesar(@PathVariable UUID id, @RequestBody ProcesoRequest request) {
        return inscripciones.procesar(id, request.getEquipoIds());
    }

    @PostMapping("/equipos/{id}/procesar")
    public InscripcionEquipoView procesarEquipo(@PathVariable UUID id) {
        return inscripciones.procesarUno(id);
    }
}
