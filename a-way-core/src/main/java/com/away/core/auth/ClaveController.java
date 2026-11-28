package com.away.core.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class ClaveController {

    private final ClaveService claves;

    public ClaveController(ClaveService claves) {
        this.claves = claves;
    }

    @GetMapping("/politica-clave")
    public ClaveViews.PoliticaView politica() {
        return claves.politica();
    }

    @PostMapping("/cambiar-clave")
    public void cambiar(@RequestBody ClaveViews.CambioRequest request) {
        claves.cambiar(request);
    }
}
