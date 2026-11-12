package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "partidos_sanciones_detalle")
public class PartidoSancionDetalle {

    @Id
    @Column(name = "id_partido_sancion_detalle")
    private UUID id;

    @Column(name = "id_partido_sancion", nullable = false)
    private UUID idPartidoSancion;

    @Column(name = "id_sancion_jugador", nullable = false)
    private UUID idSancionJugador;

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

    public UUID getIdPartidoSancion() {
        return idPartidoSancion;
    }

    public void setIdPartidoSancion(UUID idPartidoSancion) {
        this.idPartidoSancion = idPartidoSancion;
    }

    public UUID getIdSancionJugador() {
        return idSancionJugador;
    }

    public void setIdSancionJugador(UUID idSancionJugador) {
        this.idSancionJugador = idSancionJugador;
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
