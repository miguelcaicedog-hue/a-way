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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/refuerzos")
public class RefuerzoController {

    private final RefuerzoService refuerzos;

    public RefuerzoController(RefuerzoService refuerzos) {
        this.refuerzos = refuerzos;
    }

    @GetMapping
    public List<RefuerzoArchivoView> listar(@RequestParam UUID leagueId, @RequestParam UUID editionId) {
        return refuerzos.listar(leagueId, editionId);
    }

    @GetMapping("/{id}")
    public RefuerzoDetalleView detalle(@PathVariable UUID id) {
        return refuerzos.detalle(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        refuerzos.eliminar(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RefuerzoDetalleView cargar(@RequestParam UUID leagueId,
                                      @RequestParam UUID editionId,
                                      @RequestParam("archivo") MultipartFile archivo) {
        return refuerzos.cargar(leagueId, editionId, archivo);
    }

    @GetMapping("/equipos/{id}/jugadores")
    public List<RefuerzoJugadorView> jugadores(@PathVariable UUID id) {
        return refuerzos.jugadores(id);
    }

    @PutMapping("/equipos/{id}/seleccion")
    public RefuerzoEquipoView seleccionar(@PathVariable UUID id, @RequestBody SeleccionRequest request) {
        return refuerzos.seleccionar(id, request.isSeleccionado());
    }

    @GetMapping("/jugadores/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable UUID id) {
        RefuerzoService.FotoContenido contenido = refuerzos.foto(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(contenido.tipo))
                .body(contenido.bytes);
    }

    @PutMapping(value = "/jugadores/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RefuerzoJugadorView editarJugador(@PathVariable UUID id,
                                             @RequestParam String cedula,
                                             @RequestParam String nombre,
                                             @RequestParam(required = false) String alias,
                                             @RequestParam String numeroCamisa,
                                             @RequestParam(required = false) String numeroCamisaReemplazo,
                                             @RequestParam(value = "foto", required = false) MultipartFile foto) {
        return refuerzos.editarJugador(id, cedula, nombre, alias, numeroCamisa, numeroCamisaReemplazo, foto);
    }

    @PostMapping("/{id}/acceso-fotos")
    public AccesoFotosView accesoFotos(@PathVariable UUID id, @RequestBody ProcesoRequest request) {
        return refuerzos.accesoFotos(id, request.getEquipoIds());
    }

    @PostMapping("/{id}/procesar")
    public RefuerzoDetalleView procesar(@PathVariable UUID id, @RequestBody ProcesoRequest request) {
        return refuerzos.procesar(id, request.getEquipoIds());
    }

    @PostMapping("/equipos/{id}/procesar")
    public RefuerzoEquipoView procesarEquipo(@PathVariable UUID id) {
        return refuerzos.procesarUno(id);
    }
}
