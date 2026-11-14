package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "formatos_publicacion_tabla")
public class FormatoPublicacionTabla {

    @Id
    @Column(name = "id_formato_publicacion_tabla")
    private UUID id;

    @Column(name = "id_formato_publicacion", nullable = false)
    private UUID idFormatoPublicacion;

    @Column(name = "id_liga", nullable = false)
    private UUID idLiga;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "tabla_x", nullable = false)
    private int tablaX;

    @Column(name = "tabla_y", nullable = false)
    private int tablaY;

    @Column(name = "tabla_ancho", nullable = false)
    private int tablaAncho;

    @Column(name = "borde_grosor", nullable = false)
    private int bordeGrosor;

    @Column(name = "borde_color", nullable = false, length = 7)
    private String bordeColor;

    @Column(name = "cabecera_alto", nullable = false)
    private int cabeceraAlto;

    @Column(name = "cabecera_fondo", nullable = false, length = 7)
    private String cabeceraFondo;

    @Column(name = "cabecera_color", nullable = false, length = 7)
    private String cabeceraColor;

    @Column(name = "cabecera_tamano", nullable = false)
    private int cabeceraTamano;

    @Column(name = "fila_alto", nullable = false)
    private int filaAlto;

    @Column(name = "fila_fondo", nullable = false, length = 7)
    private String filaFondo;

    @Column(name = "fila_color", nullable = false, length = 7)
    private String filaColor;

    @Column(name = "fila_tamano", nullable = false)
    private int filaTamano;

    @Column(name = "numero_color", nullable = false, length = 7)
    private String numeroColor;

    @Column(name = "numero_tamano", nullable = false)
    private int numeroTamano;

    @Column(name = "destacada_fondo", nullable = false, length = 7)
    private String destacadaFondo;

    @Column(name = "columnas_tabla", nullable = false, length = 120)
    private String columnasTabla;

    @Column(nullable = false, length = 80)
    private String titulo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private OffsetDateTime fechaModificacion;

    @Column(name = "usuario_creacion", nullable = false, updatable = false, length = 80)
    private String usuarioCreacion;

    @Column(name = "usuario_modificacion", nullable = false, length = 80)
    private String usuarioModificacion;

    @Column(nullable = false, length = 20)
    private String estado;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdFormatoPublicacion() {
        return idFormatoPublicacion;
    }

    public void setIdFormatoPublicacion(UUID idFormatoPublicacion) {
        this.idFormatoPublicacion = idFormatoPublicacion;
    }

    public UUID getIdLiga() {
        return idLiga;
    }

    public void setIdLiga(UUID idLiga) {
        this.idLiga = idLiga;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getTablaX() {
        return tablaX;
    }

    public void setTablaX(int tablaX) {
        this.tablaX = tablaX;
    }

    public int getTablaY() {
        return tablaY;
    }

    public void setTablaY(int tablaY) {
        this.tablaY = tablaY;
    }

    public int getTablaAncho() {
        return tablaAncho;
    }

    public void setTablaAncho(int tablaAncho) {
        this.tablaAncho = tablaAncho;
    }

    public int getBordeGrosor() {
        return bordeGrosor;
    }

    public void setBordeGrosor(int bordeGrosor) {
        this.bordeGrosor = bordeGrosor;
    }

    public String getBordeColor() {
        return bordeColor;
    }

    public void setBordeColor(String bordeColor) {
        this.bordeColor = bordeColor;
    }

    public int getCabeceraAlto() {
        return cabeceraAlto;
    }

    public void setCabeceraAlto(int cabeceraAlto) {
        this.cabeceraAlto = cabeceraAlto;
    }

    public String getCabeceraFondo() {
        return cabeceraFondo;
    }

    public void setCabeceraFondo(String cabeceraFondo) {
        this.cabeceraFondo = cabeceraFondo;
    }

    public String getCabeceraColor() {
        return cabeceraColor;
    }

    public void setCabeceraColor(String cabeceraColor) {
        this.cabeceraColor = cabeceraColor;
    }

    public int getCabeceraTamano() {
        return cabeceraTamano;
    }

    public void setCabeceraTamano(int cabeceraTamano) {
        this.cabeceraTamano = cabeceraTamano;
    }

    public int getFilaAlto() {
        return filaAlto;
    }

    public void setFilaAlto(int filaAlto) {
        this.filaAlto = filaAlto;
    }

    public String getFilaFondo() {
        return filaFondo;
    }

    public void setFilaFondo(String filaFondo) {
        this.filaFondo = filaFondo;
    }

    public String getFilaColor() {
        return filaColor;
    }

    public void setFilaColor(String filaColor) {
        this.filaColor = filaColor;
    }

    public int getFilaTamano() {
        return filaTamano;
    }

    public void setFilaTamano(int filaTamano) {
        this.filaTamano = filaTamano;
    }

    public String getNumeroColor() {
        return numeroColor;
    }

    public void setNumeroColor(String numeroColor) {
        this.numeroColor = numeroColor;
    }

    public int getNumeroTamano() {
        return numeroTamano;
    }

    public void setNumeroTamano(int numeroTamano) {
        this.numeroTamano = numeroTamano;
    }

    public String getDestacadaFondo() {
        return destacadaFondo;
    }

    public void setDestacadaFondo(String destacadaFondo) {
        this.destacadaFondo = destacadaFondo;
    }

    public String getColumnasTabla() {
        return columnasTabla;
    }

    public void setColumnasTabla(String columnasTabla) {
        this.columnasTabla = columnasTabla;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
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
