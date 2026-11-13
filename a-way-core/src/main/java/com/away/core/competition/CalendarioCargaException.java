package com.away.core.competition;

import java.util.List;

public class CalendarioCargaException extends RuntimeException {

    private final List<String> equipos;

    CalendarioCargaException(String categoria, String grupo, List<String> equipos) {
        super("El archivo no se puede cargar. Estos equipos no están en " + categoria + " / " + grupo + ".");
        this.equipos = List.copyOf(equipos);
    }

    public List<String> getEquipos() {
        return equipos;
    }
}
