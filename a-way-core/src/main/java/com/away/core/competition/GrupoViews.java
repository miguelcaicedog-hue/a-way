package com.away.core.competition;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

class GrupoCategoriaRequest {
    @NotNull
    private UUID categoryId;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
}

class GrupoMoverRequest {
    @NotNull
    private UUID categoryId;
    @NotNull
    private UUID teamId;
    private UUID groupId;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }
}

class GrupoPanelView {
    private final UUID categoryId;
    private final String categoryName;
    private final int cantidadGrupos;
    private final Integer equiposPorGrupo;
    private final List<GrupoView> grupos;
    private final List<GrupoEquipoView> sinGrupo;

    GrupoPanelView(UUID categoryId, String categoryName, int cantidadGrupos, Integer equiposPorGrupo,
                   List<GrupoView> grupos, List<GrupoEquipoView> sinGrupo) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.cantidadGrupos = cantidadGrupos;
        this.equiposPorGrupo = equiposPorGrupo;
        this.grupos = grupos;
        this.sinGrupo = sinGrupo;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getCantidadGrupos() {
        return cantidadGrupos;
    }

    public Integer getEquiposPorGrupo() {
        return equiposPorGrupo;
    }

    public List<GrupoView> getGrupos() {
        return grupos;
    }

    public List<GrupoEquipoView> getSinGrupo() {
        return sinGrupo;
    }
}

class GrupoView {
    private final UUID id;
    private final String nombre;
    private final int orden;
    private final List<GrupoEquipoView> equipos;

    GrupoView(UUID id, String nombre, int orden, List<GrupoEquipoView> equipos) {
        this.id = id;
        this.nombre = nombre;
        this.orden = orden;
        this.equipos = equipos;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getOrden() {
        return orden;
    }

    public List<GrupoEquipoView> getEquipos() {
        return equipos;
    }
}

class GrupoEquipoView {
    private final UUID teamId;
    private final String nombre;

    GrupoEquipoView(UUID teamId, String nombre) {
        this.teamId = teamId;
        this.nombre = nombre;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getNombre() {
        return nombre;
    }
}
