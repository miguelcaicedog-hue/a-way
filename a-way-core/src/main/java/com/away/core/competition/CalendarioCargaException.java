package com.away.core.competition;

import java.util.List;

public class CalendarioCargaException extends RuntimeException {

    private final List<String> equipos;

    CalendarioCargaException(List<String> equipos) {
        super("El archivo no se puede cargar.");
        this.equipos = List.copyOf(equipos);
    }

    public List<String> getEquipos() {
        return equipos;
    }
}
