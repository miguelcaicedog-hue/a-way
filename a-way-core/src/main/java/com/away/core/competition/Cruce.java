package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cruces")
public class Cruce {

    @Id
    @Column(name = "id_cruce")
    private UUID id;

    @Column(name = "id_fase", nullable = false)
    private UUID idFase;

    @Column(name = "id_categoria", nullable = false)
    private UUID idCategoria;

    @Column(nullable = false)
    private int orden;

    @Column(name = "posicion_local")
    private Integer posicionLocal;

    @Column(name = "id_grupo_local")
    private UUID idGrupoLocal;

    @Column(name = "posicion_visita")
    private Integer posicionVisita;

    @Column(name = "id_grupo_visita")
    private UUID idGrupoVisita;

    @Column(name = "id_cruce_origen_local")
    private UUID idCruceOrigenLocal;

    @Column(name = "id_cruce_origen_visita")
    private UUID idCruceOrigenVisita;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private OffsetDateTime fechaModificacion;

    @Column(name = "usuario_creacion", nullable = false, updatable = false)
    private String usuarioCreacion;

    @Column(name = "usuario_modificacion", nullable = false)
    private String usuarioModificacion;

    @Column(nullable = false)
    private String estado;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdFase() {
        return idFase;
    }

    public void setIdFase(UUID idFase) {
        this.idFase = idFase;
    }

    public UUID getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(UUID idCategoria) {
        this.idCategoria = idCategoria;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public Integer getPosicionLocal() {
        return posicionLocal;
    }

    public void setPosicionLocal(Integer posicionLocal) {
        this.posicionLocal = posicionLocal;
    }

    public UUID getIdGrupoLocal() {
        return idGrupoLocal;
    }

    public void setIdGrupoLocal(UUID idGrupoLocal) {
        this.idGrupoLocal = idGrupoLocal;
    }

    public Integer getPosicionVisita() {
        return posicionVisita;
    }

    public void setPosicionVisita(Integer posicionVisita) {
        this.posicionVisita = posicionVisita;
    }

    public UUID getIdGrupoVisita() {
        return idGrupoVisita;
    }

    public void setIdGrupoVisita(UUID idGrupoVisita) {
        this.idGrupoVisita = idGrupoVisita;
    }

    public UUID getIdCruceOrigenLocal() {
        return idCruceOrigenLocal;
    }

    public void setIdCruceOrigenLocal(UUID idCruceOrigenLocal) {
        this.idCruceOrigenLocal = idCruceOrigenLocal;
    }

    public UUID getIdCruceOrigenVisita() {
        return idCruceOrigenVisita;
    }

    public void setIdCruceOrigenVisita(UUID idCruceOrigenVisita) {
        this.idCruceOrigenVisita = idCruceOrigenVisita;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(OffsetDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public void setUsuarioCreacion(String usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
