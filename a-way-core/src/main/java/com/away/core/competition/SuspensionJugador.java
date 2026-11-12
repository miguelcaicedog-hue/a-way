package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "suspensiones_jugador")
public class SuspensionJugador {

    @Id
    @Column(name = "id_suspension_jugador")
    private UUID id;

    @Column(name = "id_liga", nullable = false)
    private UUID idLiga;

    @Column(name = "id_categoria", nullable = false)
    private UUID idCategoria;

    @Column(name = "id_jugador", nullable = false)
    private UUID idJugador;

    @Column(name = "id_jornada_origen", nullable = false)
    private UUID idJornadaOrigen;

    @Column(name = "id_jornada_inicio", nullable = false)
    private UUID idJornadaInicio;

    @Column(nullable = false)
    private int jornadas;

    @Column(nullable = false)
    private String motivo;

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

    public UUID getIdLiga() {
        return idLiga;
    }

    public void setIdLiga(UUID idLiga) {
        this.idLiga = idLiga;
    }

    public UUID getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(UUID idCategoria) {
        this.idCategoria = idCategoria;
    }

    public UUID getIdJugador() {
        return idJugador;
    }

    public void setIdJugador(UUID idJugador) {
        this.idJugador = idJugador;
    }

    public UUID getIdJornadaOrigen() {
        return idJornadaOrigen;
    }

    public void setIdJornadaOrigen(UUID idJornadaOrigen) {
        this.idJornadaOrigen = idJornadaOrigen;
    }

    public UUID getIdJornadaInicio() {
        return idJornadaInicio;
    }

    public void setIdJornadaInicio(UUID idJornadaInicio) {
        this.idJornadaInicio = idJornadaInicio;
    }

    public int getJornadas() {
        return jornadas;
    }

    public void setJornadas(int jornadas) {
        this.jornadas = jornadas;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
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
