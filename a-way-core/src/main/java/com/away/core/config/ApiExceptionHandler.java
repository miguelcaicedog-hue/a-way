package com.away.core.config;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.away.core.competition.CalendarioCargaException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException exception) {
        String detail = exception.getMostSpecificCause().getMessage();
        String message = "No se pudo guardar: el nombre ya existe o hay registros que dependen de este dato.";
        if (detail != null && detail.contains("roster exceeds max_players")) {
            message = "El plantel ya alcanzó el máximo de jugadores de la categoría.";
        } else if (detail != null && detail.contains("player_entry_numero_camisa")) {
            message = "Ese número de camisa ya está usado en el equipo.";
        } else if (detail != null && (detail.contains("player_entry_cedula") || detail.contains("player_entry_number"))) {
            message = "Esa cédula ya está registrada en el equipo.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", message));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> dataAccess(DataAccessException exception) {
        String detail = exception.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("roster exceeds max_players")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("error", "El plantel ya alcanzó el máximo de jugadores de la categoría."));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("error", "No se pudo guardar: el nombre ya existe o hay registros que dependen de este dato."));
    }

    @ExceptionHandler(CalendarioCargaException.class)
    public ResponseEntity<Map<String, Object>> calendarioCarga(CalendarioCargaException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage());
        body.put("equipos", exception.getEquipos());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException exception) {
        String message = exception.getReason() == null ? "No se pudo completar la operación" : exception.getReason();
        return ResponseEntity.status(exception.getStatus()).body(Collections.singletonMap("error", message));
    }
}
