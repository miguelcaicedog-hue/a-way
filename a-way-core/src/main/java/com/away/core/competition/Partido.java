package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "partidos")
public class Partido {

    @Id
    @Column(name = "id_partido")
    private UUID id;

    @Column(name = "id_jornada", nullable = false)
    private UUID idJornada;

    @Column(name = "id_categoria", nullable = false)
    private UUID idCategoria;

    @Column(name = "id_fase", nullable = false)
    private UUID idFase;

    @Column(name = "id_grupo")
    private UUID idGrupo;

    @Column(name = "id_cruce")
    private UUID idCruce;

    @Column(name = "id_equipo_local")
    private UUID idEquipoLocal;

    @Column(name = "id_equipo_visita")
    private UUID idEquipoVisita;

    @Column(name = "posicion_local")
    private Integer posicionLocal;

    @Column(name = "id_grupo_local")
    private UUID idGrupoLocal;

    @Column(name = "posicion_visita")
    private Integer posicionVisita;

    @Column(name = "id_grupo_visita")
    private UUID idGrupoVisita;

    @Column(nullable = false)
    private int orden;

    private LocalDate fecha;

    private LocalTime hora;

    @Column(name = "id_cancha")
    private UUID idCancha;

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

    public UUID getIdJornada() {
        return idJornada;
    }

    public void setIdJornada(UUID idJornada) {
        this.idJornada = idJornada;
    }

    public UUID getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(UUID idCategoria) {
        this.idCategoria = idCategoria;
    }

    public UUID getIdFase() {
        return idFase;
    }

    public void setIdFase(UUID idFase) {
        this.idFase = idFase;
    }

    public UUID getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(UUID idGrupo) {
        this.idGrupo = idGrupo;
    }

    public UUID getIdCruce() {
        return idCruce;
    }

    public void setIdCruce(UUID idCruce) {
        this.idCruce = idCruce;
    }

    public UUID getIdEquipoLocal() {
        return idEquipoLocal;
    }

    public void setIdEquipoLocal(UUID idEquipoLocal) {
        this.idEquipoLocal = idEquipoLocal;
    }

    public UUID getIdEquipoVisita() {
        return idEquipoVisita;
    }

    public void setIdEquipoVisita(UUID idEquipoVisita) {
        this.idEquipoVisita = idEquipoVisita;
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

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    public UUID getIdCancha() {
        return idCancha;
    }

    public void setIdCancha(UUID idCancha) {
        this.idCancha = idCancha;
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
