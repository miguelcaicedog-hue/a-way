package com.away.core.competition;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

class FaseCategoriaRequest {
    @NotNull
    private UUID categoryId;

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
}

class FaseModalidadRequest {
    @NotNull
    @Pattern(regexp = "IDA|IDA_Y_VUELTA")
    private String modalidad;

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }
}

class FaseCruceRequest {
    @NotNull
    private UUID cruceId;
    @NotNull
    @Pattern(regexp = "LOCAL|VISITA")
    private String lado;
    private UUID groupId;
    @Min(1)
    private Integer posicion;

    public UUID getCruceId() {
        return cruceId;
    }

    public void setCruceId(UUID cruceId) {
        this.cruceId = cruceId;
    }

    public String getLado() {
        return lado;
    }

    public void setLado(String lado) {
        this.lado = lado;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public Integer getPosicion() {
        return posicion;
    }

    public void setPosicion(Integer posicion) {
        this.posicion = posicion;
    }
}

class FasePanelView {
    private final UUID categoryId;
    private final String categoryName;
    private final int cantidadFases;
    private final List<FaseGrupoView> grupos;
    private final List<FasePosicionView> posiciones;
    private final List<FaseView> fases;

    FasePanelView(UUID categoryId, String categoryName, int cantidadFases, List<FaseGrupoView> grupos,
                  List<FasePosicionView> posiciones, List<FaseView> fases) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.cantidadFases = cantidadFases;
        this.grupos = grupos;
        this.posiciones = posiciones;
        this.fases = fases;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getCantidadFases() {
        return cantidadFases;
    }

    public List<FaseGrupoView> getGrupos() {
        return grupos;
    }

    public List<FasePosicionView> getPosiciones() {
        return posiciones;
    }

    public List<FaseView> getFases() {
        return fases;
    }
}

class FaseGrupoView {
    private final UUID id;
    private final String nombre;

    FaseGrupoView(UUID id, String nombre) {
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

class FasePosicionView {
    private final UUID groupId;
    private final int posicion;
    private final String etiqueta;
    private final boolean ocupada;

    FasePosicionView(UUID groupId, int posicion, String etiqueta, boolean ocupada) {
        this.groupId = groupId;
        this.posicion = posicion;
        this.etiqueta = etiqueta;
        this.ocupada = ocupada;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public int getPosicion() {
        return posicion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean isOcupada() {
        return ocupada;
    }
}

class FaseView {
    private final UUID id;
    private final String nombre;
    private final int orden;
    private final String modalidad;
    private final List<CruceView> cruces;

    FaseView(UUID id, String nombre, int orden, String modalidad, List<CruceView> cruces) {
        this.id = id;
        this.nombre = nombre;
        this.orden = orden;
        this.modalidad = modalidad;
        this.cruces = cruces;
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

    public String getModalidad() {
        return modalidad;
    }

    public List<CruceView> getCruces() {
        return cruces;
    }
}

class CruceView {
    private final UUID id;
    private final int orden;
    private final boolean editable;
    private final UUID groupIdLocal;
    private final Integer posicionLocal;
    private final String textoLocal;
    private final UUID groupIdVisita;
    private final Integer posicionVisita;
    private final String textoVisita;

    CruceView(UUID id, int orden, boolean editable,
              UUID groupIdLocal, Integer posicionLocal, String textoLocal,
              UUID groupIdVisita, Integer posicionVisita, String textoVisita) {
        this.id = id;
        this.orden = orden;
        this.editable = editable;
        this.groupIdLocal = groupIdLocal;
        this.posicionLocal = posicionLocal;
        this.textoLocal = textoLocal;
        this.groupIdVisita = groupIdVisita;
        this.posicionVisita = posicionVisita;
        this.textoVisita = textoVisita;
    }

    public UUID getId() {
        return id;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isEditable() {
        return editable;
    }

    public UUID getGroupIdLocal() {
        return groupIdLocal;
    }

    public Integer getPosicionLocal() {
        return posicionLocal;
    }

    public String getTextoLocal() {
        return textoLocal;
    }

    public UUID getGroupIdVisita() {
        return groupIdVisita;
    }

    public Integer getPosicionVisita() {
        return posicionVisita;
    }

    public String getTextoVisita() {
        return textoVisita;
    }
}
