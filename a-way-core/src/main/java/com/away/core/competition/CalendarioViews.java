package com.away.core.competition;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

class CalendarioCategoriaRequest {
    @NotNull
    private UUID categoryId;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
}

class CalendarioFechaRequest {
    private LocalDate fecha;

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }
}

class CalendarioPanelView {
    private final UUID categoryId;
    private final String categoryName;
    private final UUID groupId;
    private final List<CalendarioGrupoView> grupos;
    private final List<JornadaView> jornadas;

    CalendarioPanelView(UUID categoryId, String categoryName, UUID groupId, List<CalendarioGrupoView> grupos,
                        List<JornadaView> jornadas) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.groupId = groupId;
        this.grupos = grupos;
        this.jornadas = jornadas;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public List<CalendarioGrupoView> getGrupos() {
        return grupos;
    }

    public List<JornadaView> getJornadas() {
        return jornadas;
    }
}

class CalendarioGrupoView {
    private final UUID id;
    private final String nombre;

    CalendarioGrupoView(UUID id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

class JornadaView {
    private final UUID id;
    private final int numero;
    private final String nombre;
    private final LocalDate fecha;
    private final List<PartidoView> partidos;

    JornadaView(UUID id, int numero, String nombre, LocalDate fecha, List<PartidoView> partidos) {
        this.id = id;
        this.numero = numero;
        this.nombre = nombre;
        this.fecha = fecha;
        this.partidos = partidos;
    }

    public UUID getId() {
        return id;
    }

    public int getNumero() {
        return numero;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public List<PartidoView> getPartidos() {
        return partidos;
    }
}

class PartidoView {
    private final UUID id;
    private final int orden;
    private final String textoLocal;
    private final String textoVisita;
    private final LocalDate fecha;

    PartidoView(UUID id, int orden, String textoLocal, String textoVisita, LocalDate fecha) {
        this.id = id;
        this.orden = orden;
        this.textoLocal = textoLocal;
        this.textoVisita = textoVisita;
        this.fecha = fecha;
    }

    public UUID getId() {
        return id;
    }

    public int getOrden() {
        return orden;
    }

    public String getTextoLocal() {
        return textoLocal;
    }

    public String getTextoVisita() {
        return textoVisita;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
