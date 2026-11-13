package com.away.core.competition;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    public CalendarioPanelView panel(@RequestParam UUID categoryId,
                                     @RequestParam(required = false) UUID groupId,
                                     @RequestParam(defaultValue = "false") boolean todos) {
        return calendario.panel(categoryId, groupId, todos);
    }

    @PostMapping("/generar")
    public CalendarioPanelView generar(@Valid @RequestBody CalendarioCategoriaRequest request,
                                       @RequestParam(required = false) UUID groupId,
                                       @RequestParam(defaultValue = "false") boolean todos) {
        return calendario.generar(request.getCategoryId(), groupId, todos);
    }

    @PostMapping(value = "/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CalendarioPanelView cargar(@RequestParam UUID categoryId,
                                      @RequestParam UUID groupId,
                                      @RequestParam("archivo") MultipartFile archivo) {
        return calendario.cargar(categoryId, groupId, archivo);
    }

    @PutMapping("/jornada/{id}")
    public CalendarioPanelView fechaJornada(@PathVariable UUID id,
                                            @RequestBody CalendarioFechaRequest request,
                                            @RequestParam(required = false) UUID groupId,
                                            @RequestParam(defaultValue = "false") boolean todos) {
        return calendario.fechaJornada(id, request.getFecha(), groupId, todos);
    }

    @PutMapping("/partido/{id}")
    public CalendarioPanelView fechaPartido(@PathVariable UUID id,
                                            @RequestBody CalendarioFechaRequest request,
                                            @RequestParam(required = false) UUID groupId,
                                            @RequestParam(defaultValue = "false") boolean todos) {
        return calendario.fechaPartido(id, request.getFecha(), groupId, todos);
    }

    @DeleteMapping
    public CalendarioPanelView eliminar(@RequestParam UUID categoryId,
                                        @RequestParam(required = false) UUID groupId,
                                        @RequestParam(defaultValue = "false") boolean todos) {
        return calendario.eliminar(categoryId, groupId, todos);
    }
}
