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

    @Column(name = "titulo_x", nullable = false)
    private int tituloX;

    @Column(name = "titulo_y", nullable = false)
    private int tituloY;

    @Column(name = "titulo_tamano", nullable = false)
    private int tituloTamano;

    @Column(name = "subtitulo_x", nullable = false)
    private int subtituloX;

    @Column(name = "subtitulo_y", nullable = false)
    private int subtituloY;

    @Column(name = "subtitulo_tamano", nullable = false)
    private int subtituloTamano;

    @Column(name = "fecha_x", nullable = false)
    private int fechaX;

    @Column(name = "fecha_y", nullable = false)
    private int fechaY;

    @Column(name = "fecha_tamano", nullable = false)
    private int fechaTamano;

    @Column(name = "lugar_y", nullable = false)
    private int lugarY;

    @Column(name = "lugar_tamano", nullable = false)
    private int lugarTamano;

    @Column(name = "agrupar_primera_columna", nullable = false)
    private boolean agruparPrimeraColumna;

    @Column(name = "grupo_familia", nullable = false, length = 40)
    private String grupoFamilia;

    @Column(name = "grupo_tamano", nullable = false)
    private int grupoTamano;

    @Column(name = "grupo_alto", nullable = false)
    private int grupoAlto;

    @Column(name = "grupo_fondo", nullable = false, length = 7)
    private String grupoFondo;

    @Column(name = "grupo_color", nullable = false, length = 7)
    private String grupoColor;

    @Column(name = "ajustar_ancho_texto", nullable = false)
    private boolean ajustarAnchoTexto;

    @Column(name = "fondo_publicacion", length = 80)
    private String fondoPublicacion;

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

    public int getTituloX() {
        return tituloX;
    }

    public void setTituloX(int tituloX) {
        this.tituloX = tituloX;
    }

    public int getTituloY() {
        return tituloY;
    }

    public void setTituloY(int tituloY) {
        this.tituloY = tituloY;
    }

    public int getTituloTamano() {
        return tituloTamano;
    }

    public void setTituloTamano(int tituloTamano) {
        this.tituloTamano = tituloTamano;
    }

    public int getSubtituloX() {
        return subtituloX;
    }

    public void setSubtituloX(int subtituloX) {
        this.subtituloX = subtituloX;
    }

    public int getSubtituloY() {
        return subtituloY;
    }

    public void setSubtituloY(int subtituloY) {
        this.subtituloY = subtituloY;
    }

    public int getSubtituloTamano() {
        return subtituloTamano;
    }

    public void setSubtituloTamano(int subtituloTamano) {
        this.subtituloTamano = subtituloTamano;
    }

    public int getFechaX() {
        return fechaX;
    }

    public void setFechaX(int fechaX) {
        this.fechaX = fechaX;
    }

    public int getFechaY() {
        return fechaY;
    }

    public void setFechaY(int fechaY) {
        this.fechaY = fechaY;
    }

    public int getFechaTamano() {
        return fechaTamano;
    }

    public void setFechaTamano(int fechaTamano) {
        this.fechaTamano = fechaTamano;
    }

    public int getLugarY() {
        return lugarY;
    }

    public void setLugarY(int lugarY) {
        this.lugarY = lugarY;
    }

    public int getLugarTamano() {
        return lugarTamano;
    }

    public void setLugarTamano(int lugarTamano) {
        this.lugarTamano = lugarTamano;
    }

    public boolean isAgruparPrimeraColumna() {
        return agruparPrimeraColumna;
    }

    public void setAgruparPrimeraColumna(boolean agruparPrimeraColumna) {
        this.agruparPrimeraColumna = agruparPrimeraColumna;
    }

    public String getGrupoFamilia() {
        return grupoFamilia;
    }

    public void setGrupoFamilia(String grupoFamilia) {
        this.grupoFamilia = grupoFamilia;
    }

    public int getGrupoTamano() {
        return grupoTamano;
    }

    public void setGrupoTamano(int grupoTamano) {
        this.grupoTamano = grupoTamano;
    }

    public int getGrupoAlto() {
        return grupoAlto;
    }

    public void setGrupoAlto(int grupoAlto) {
        this.grupoAlto = grupoAlto;
    }

    public String getGrupoFondo() {
        return grupoFondo;
    }

    public void setGrupoFondo(String grupoFondo) {
        this.grupoFondo = grupoFondo;
    }

    public String getGrupoColor() {
        return grupoColor;
    }

    public void setGrupoColor(String grupoColor) {
        this.grupoColor = grupoColor;
    }

    public boolean isAjustarAnchoTexto() {
        return ajustarAnchoTexto;
    }

    public void setAjustarAnchoTexto(boolean ajustarAnchoTexto) {
        this.ajustarAnchoTexto = ajustarAnchoTexto;
    }

    public String getFondoPublicacion() {
        return fondoPublicacion;
    }

    public void setFondoPublicacion(String fondoPublicacion) {
        this.fondoPublicacion = fondoPublicacion;
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
