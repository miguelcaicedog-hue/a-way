package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "partidos_cobros_detalle")
public class PartidoCobroDetalle {

    @Id
    @Column(name = "id_partido_cobro_detalle")
    private UUID id;

    @Column(name = "id_partido_cobro", nullable = false)
    private UUID idPartidoCobro;

    @Column(name = "id_partido", nullable = false)
    private UUID idPartido;

    @Column(name = "id_jornada", nullable = false)
    private UUID idJornada;

    private LocalDate fecha;

    @Column(name = "id_equipo", nullable = false)
    private UUID idEquipo;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costo;

    @Column(name = "id_arbitro")
    private UUID idArbitro;

    @Column(name = "id_tarjeta_jugador")
    private UUID idTarjetaJugador;

    @Column(name = "id_partido_sancion_detalle")
    private UUID idPartidoSancionDetalle;

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

    public UUID getIdPartidoCobro() {
        return idPartidoCobro;
    }

    public void setIdPartidoCobro(UUID idPartidoCobro) {
        this.idPartidoCobro = idPartidoCobro;
    }

    public UUID getIdPartido() {
        return idPartido;
    }

    public void setIdPartido(UUID idPartido) {
        this.idPartido = idPartido;
    }

    public UUID getIdJornada() {
        return idJornada;
    }

    public void setIdJornada(UUID idJornada) {
        this.idJornada = idJornada;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public UUID getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(UUID idEquipo) {
        this.idEquipo = idEquipo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public UUID getIdArbitro() {
        return idArbitro;
    }

    public void setIdArbitro(UUID idArbitro) {
        this.idArbitro = idArbitro;
    }

    public UUID getIdTarjetaJugador() {
        return idTarjetaJugador;
    }

    public void setIdTarjetaJugador(UUID idTarjetaJugador) {
        this.idTarjetaJugador = idTarjetaJugador;
    }

    public UUID getIdPartidoSancionDetalle() {
        return idPartidoSancionDetalle;
    }

    public void setIdPartidoSancionDetalle(UUID idPartidoSancionDetalle) {
        this.idPartidoSancionDetalle = idPartidoSancionDetalle;
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
