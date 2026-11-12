package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "arbitros")
public class Arbitro {

    @Id
    @Column(name = "id_arbitro")
    private UUID id;

    @Column(name = "id_liga", nullable = false)
    private UUID idLiga;

    private String cedula;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    @Column(name = "valor_pago_partido", nullable = false)
    private BigDecimal valorPagoPartido;

    @Column(name = "valor_cobro_equipo", nullable = false)
    private BigDecimal valorCobroEquipo;

    @Column(name = "valor_pago_sin_pito", nullable = false)
    private BigDecimal valorPagoSinPito;

    private byte[] foto;

    @Column(name = "foto_content_type")
    private String fotoContentType;

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

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public BigDecimal getValorPagoPartido() {
        return valorPagoPartido;
    }

    public void setValorPagoPartido(BigDecimal valorPagoPartido) {
        this.valorPagoPartido = valorPagoPartido;
    }

    public BigDecimal getValorCobroEquipo() {
        return valorCobroEquipo;
    }

    public void setValorCobroEquipo(BigDecimal valorCobroEquipo) {
        this.valorCobroEquipo = valorCobroEquipo;
    }

    public BigDecimal getValorPagoSinPito() {
        return valorPagoSinPito;
    }

    public void setValorPagoSinPito(BigDecimal valorPagoSinPito) {
        this.valorPagoSinPito = valorPagoSinPito;
    }

    public byte[] getFoto() {
        return foto;
    }

    public void setFoto(byte[] foto) {
        this.foto = foto;
    }

    public String getFotoContentType() {
        return fotoContentType;
    }

    public void setFotoContentType(String fotoContentType) {
        this.fotoContentType = fotoContentType;
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
