package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reglas_suspension")
public class ReglaSuspension {

    @Id
    @Column(name = "id_regla_suspension")
    private UUID id;

    @Column(name = "id_liga", nullable = false)
    private UUID idLiga;

    @Column(name = "roja_jornadas", nullable = false)
    private int rojaJornadas;

    @Column(name = "amarillas_acumuladas", nullable = false)
    private int amarillasAcumuladas;

    @Column(name = "amarillas_jornadas", nullable = false)
    private int amarillasJornadas;

    @Column(name = "amarillas_momento", nullable = false)
    private String amarillasMomento;

    @Column(name = "rojas_acumuladas", nullable = false)
    private int rojasAcumuladas;

    @Column(name = "rojas_jornadas", nullable = false)
    private int rojasJornadas;

    @Column(name = "rojas_momento", nullable = false)
    private String rojasMomento;

    @Column(name = "falta_media_jornadas", nullable = false)
    private int faltaMediaJornadas;

    @Column(name = "estado_roja", nullable = false)
    private String estadoRoja;

    @Column(name = "estado_amarillas", nullable = false)
    private String estadoAmarillas;

    @Column(name = "estado_rojas", nullable = false)
    private String estadoRojas;

    @Column(name = "estado_falta_media", nullable = false)
    private String estadoFaltaMedia;

    @Column(name = "estado_falta_grave", nullable = false)
    private String estadoFaltaGrave;

    @Column(name = "estado_sancion", nullable = false)
    private String estadoSancion;

    @Column(name = "sancion_jornadas", nullable = false)
    private int sancionJornadas;

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

    public int getRojaJornadas() {
        return rojaJornadas;
    }

    public void setRojaJornadas(int rojaJornadas) {
        this.rojaJornadas = rojaJornadas;
    }

    public int getAmarillasAcumuladas() {
        return amarillasAcumuladas;
    }

    public void setAmarillasAcumuladas(int amarillasAcumuladas) {
        this.amarillasAcumuladas = amarillasAcumuladas;
    }

    public int getAmarillasJornadas() {
        return amarillasJornadas;
    }

    public void setAmarillasJornadas(int amarillasJornadas) {
        this.amarillasJornadas = amarillasJornadas;
    }

    public String getAmarillasMomento() {
        return amarillasMomento;
    }

    public void setAmarillasMomento(String amarillasMomento) {
        this.amarillasMomento = amarillasMomento;
    }

    public int getRojasAcumuladas() {
        return rojasAcumuladas;
    }

    public void setRojasAcumuladas(int rojasAcumuladas) {
        this.rojasAcumuladas = rojasAcumuladas;
    }

    public int getRojasJornadas() {
        return rojasJornadas;
    }

    public void setRojasJornadas(int rojasJornadas) {
        this.rojasJornadas = rojasJornadas;
    }

    public String getRojasMomento() {
        return rojasMomento;
    }

    public void setRojasMomento(String rojasMomento) {
        this.rojasMomento = rojasMomento;
    }

    public int getFaltaMediaJornadas() {
        return faltaMediaJornadas;
    }

    public void setFaltaMediaJornadas(int faltaMediaJornadas) {
        this.faltaMediaJornadas = faltaMediaJornadas;
    }

    public String getEstadoRoja() {
        return estadoRoja;
    }

    public void setEstadoRoja(String estadoRoja) {
        this.estadoRoja = estadoRoja;
    }

    public String getEstadoAmarillas() {
        return estadoAmarillas;
    }

    public void setEstadoAmarillas(String estadoAmarillas) {
        this.estadoAmarillas = estadoAmarillas;
    }

    public String getEstadoRojas() {
        return estadoRojas;
    }

    public void setEstadoRojas(String estadoRojas) {
        this.estadoRojas = estadoRojas;
    }

    public String getEstadoFaltaMedia() {
        return estadoFaltaMedia;
    }

    public void setEstadoFaltaMedia(String estadoFaltaMedia) {
        this.estadoFaltaMedia = estadoFaltaMedia;
    }

    public String getEstadoFaltaGrave() {
        return estadoFaltaGrave;
    }

    public void setEstadoFaltaGrave(String estadoFaltaGrave) {
        this.estadoFaltaGrave = estadoFaltaGrave;
    }

    public String getEstadoSancion() {
        return estadoSancion;
    }

    public void setEstadoSancion(String estadoSancion) {
        this.estadoSancion = estadoSancion;
    }

    public int getSancionJornadas() {
        return sancionJornadas;
    }

    public void setSancionJornadas(int sancionJornadas) {
        this.sancionJornadas = sancionJornadas;
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
