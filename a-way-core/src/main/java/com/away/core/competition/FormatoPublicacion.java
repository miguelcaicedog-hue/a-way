package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "formatos_publicacion")
public class FormatoPublicacion {

    @Id
    @Column(name = "id_formato_publicacion")
    private UUID id;

    @Column(name = "id_liga", nullable = false)
    private UUID idLiga;

    @Column(name = "nombre_lugar", nullable = false, length = 80)
    private String nombreLugar;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(name = "logo_x", nullable = false)
    private int logoX;

    @Column(name = "logo_y", nullable = false)
    private int logoY;

    @Column(name = "logo_ancho", nullable = false)
    private int logoAncho;

    @Column(name = "logo_alto", nullable = false)
    private int logoAlto;

    @Column(name = "logo_habilitado", nullable = false)
    private boolean logoHabilitado = true;

    @Column(name = "titulo_habilitado", nullable = false)
    private boolean tituloHabilitado = true;

    @Column(name = "subtitulo_habilitado", nullable = false)
    private boolean subtituloHabilitado = true;

    @Column(name = "lugar_habilitado", nullable = false)
    private boolean lugarHabilitado = true;

    @Column(name = "fecha_habilitado", nullable = false)
    private boolean fechaHabilitado = true;

    @Column(name = "titulo_negrita", nullable = false)
    private boolean tituloNegrita;

    @Column(name = "titulo_cursiva", nullable = false)
    private boolean tituloCursiva;

    @Column(name = "titulo_familia", nullable = false, length = 40)
    private String tituloFamilia;

    @Column(name = "titulo_color_inicio", nullable = false, length = 7)
    private String tituloColorInicio;

    @Column(name = "titulo_color_fin", nullable = false, length = 7)
    private String tituloColorFin;

    @Column(name = "titulo_contorno_color", nullable = false, length = 7)
    private String tituloContornoColor;

    @Column(name = "titulo_contorno_grosor", nullable = false)
    private int tituloContornoGrosor;

    @Column(name = "titulo_sombra", nullable = false)
    private boolean tituloSombra;

    @Column(name = "subtitulo_negrita", nullable = false)
    private boolean subtituloNegrita;

    @Column(name = "subtitulo_cursiva", nullable = false)
    private boolean subtituloCursiva;

    @Column(name = "subtitulo_familia", nullable = false, length = 40)
    private String subtituloFamilia;

    @Column(name = "subtitulo_color", nullable = false, length = 7)
    private String subtituloColor;

    @Column(name = "lugar_cursiva", nullable = false)
    private boolean lugarCursiva;

    @Column(name = "lugar_familia", nullable = false, length = 40)
    private String lugarFamilia;

    @Column(name = "lugar_color", nullable = false, length = 7)
    private String lugarColor;

    @Column(name = "lugar_raya_ancho", nullable = false)
    private int lugarRayaAncho;

    @Column(name = "lugar_raya_grosor", nullable = false)
    private int lugarRayaGrosor;

    @Column(name = "fecha_ancho", nullable = false)
    private int fechaAncho;

    @Column(name = "fecha_alto", nullable = false)
    private int fechaAlto;

    @Column(name = "fecha_radio", nullable = false)
    private int fechaRadio;

    @Column(name = "fecha_borde_grosor", nullable = false)
    private int fechaBordeGrosor;

    @Column(name = "fecha_borde_color", nullable = false, length = 7)
    private String fechaBordeColor;

    @Column(name = "fecha_negrita", nullable = false)
    private boolean fechaNegrita;

    @Column(name = "fecha_familia", nullable = false, length = 40)
    private String fechaFamilia;

    @Column(name = "fecha_color", nullable = false, length = 7)
    private String fechaColor;

    @Column(name = "fecha_icono_tamano", nullable = false)
    private int fechaIconoTamano;

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

    public UUID getIdLiga() {
        return idLiga;
    }

    public void setIdLiga(UUID idLiga) {
        this.idLiga = idLiga;
    }

    public String getNombreLugar() {
        return nombreLugar;
    }

    public void setNombreLugar(String nombreLugar) {
        this.nombreLugar = nombreLugar;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getLogoX() {
        return logoX;
    }

    public void setLogoX(int logoX) {
        this.logoX = logoX;
    }

    public int getLogoY() {
        return logoY;
    }

    public void setLogoY(int logoY) {
        this.logoY = logoY;
    }

    public int getLogoAncho() {
        return logoAncho;
    }

    public void setLogoAncho(int logoAncho) {
        this.logoAncho = logoAncho;
    }

    public int getLogoAlto() {
        return logoAlto;
    }

    public void setLogoAlto(int logoAlto) {
        this.logoAlto = logoAlto;
    }

    public boolean isLogoHabilitado() {
        return logoHabilitado;
    }

    public void setLogoHabilitado(boolean logoHabilitado) {
        this.logoHabilitado = logoHabilitado;
    }

    public boolean isTituloHabilitado() {
        return tituloHabilitado;
    }

    public void setTituloHabilitado(boolean tituloHabilitado) {
        this.tituloHabilitado = tituloHabilitado;
    }

    public boolean isSubtituloHabilitado() {
        return subtituloHabilitado;
    }

    public void setSubtituloHabilitado(boolean subtituloHabilitado) {
        this.subtituloHabilitado = subtituloHabilitado;
    }

    public boolean isLugarHabilitado() {
        return lugarHabilitado;
    }

    public void setLugarHabilitado(boolean lugarHabilitado) {
        this.lugarHabilitado = lugarHabilitado;
    }

    public boolean isFechaHabilitado() {
        return fechaHabilitado;
    }

    public void setFechaHabilitado(boolean fechaHabilitado) {
        this.fechaHabilitado = fechaHabilitado;
    }

    public boolean isTituloNegrita() {
        return tituloNegrita;
    }

    public void setTituloNegrita(boolean tituloNegrita) {
        this.tituloNegrita = tituloNegrita;
    }

    public boolean isTituloCursiva() {
        return tituloCursiva;
    }

    public void setTituloCursiva(boolean tituloCursiva) {
        this.tituloCursiva = tituloCursiva;
    }

    public String getTituloFamilia() {
        return tituloFamilia;
    }

    public void setTituloFamilia(String tituloFamilia) {
        this.tituloFamilia = tituloFamilia;
    }

    public String getTituloColorInicio() {
        return tituloColorInicio;
    }

    public void setTituloColorInicio(String tituloColorInicio) {
        this.tituloColorInicio = tituloColorInicio;
    }

    public String getTituloColorFin() {
        return tituloColorFin;
    }

    public void setTituloColorFin(String tituloColorFin) {
        this.tituloColorFin = tituloColorFin;
    }

    public String getTituloContornoColor() {
        return tituloContornoColor;
    }

    public void setTituloContornoColor(String tituloContornoColor) {
        this.tituloContornoColor = tituloContornoColor;
    }

    public int getTituloContornoGrosor() {
        return tituloContornoGrosor;
    }

    public void setTituloContornoGrosor(int tituloContornoGrosor) {
        this.tituloContornoGrosor = tituloContornoGrosor;
    }

    public boolean isTituloSombra() {
        return tituloSombra;
    }

    public void setTituloSombra(boolean tituloSombra) {
        this.tituloSombra = tituloSombra;
    }

    public boolean isSubtituloNegrita() {
        return subtituloNegrita;
    }

    public void setSubtituloNegrita(boolean subtituloNegrita) {
        this.subtituloNegrita = subtituloNegrita;
    }

    public boolean isSubtituloCursiva() {
        return subtituloCursiva;
    }

    public void setSubtituloCursiva(boolean subtituloCursiva) {
        this.subtituloCursiva = subtituloCursiva;
    }

    public String getSubtituloFamilia() {
        return subtituloFamilia;
    }

    public void setSubtituloFamilia(String subtituloFamilia) {
        this.subtituloFamilia = subtituloFamilia;
    }

    public String getSubtituloColor() {
        return subtituloColor;
    }

    public void setSubtituloColor(String subtituloColor) {
        this.subtituloColor = subtituloColor;
    }

    public boolean isLugarCursiva() {
        return lugarCursiva;
    }

    public void setLugarCursiva(boolean lugarCursiva) {
        this.lugarCursiva = lugarCursiva;
    }

    public String getLugarFamilia() {
        return lugarFamilia;
    }

    public void setLugarFamilia(String lugarFamilia) {
        this.lugarFamilia = lugarFamilia;
    }

    public String getLugarColor() {
        return lugarColor;
    }

    public void setLugarColor(String lugarColor) {
        this.lugarColor = lugarColor;
    }

    public int getLugarRayaAncho() {
        return lugarRayaAncho;
    }

    public void setLugarRayaAncho(int lugarRayaAncho) {
        this.lugarRayaAncho = lugarRayaAncho;
    }

    public int getLugarRayaGrosor() {
        return lugarRayaGrosor;
    }

    public void setLugarRayaGrosor(int lugarRayaGrosor) {
        this.lugarRayaGrosor = lugarRayaGrosor;
    }

    public int getFechaAncho() {
        return fechaAncho;
    }

    public void setFechaAncho(int fechaAncho) {
        this.fechaAncho = fechaAncho;
    }

    public int getFechaAlto() {
        return fechaAlto;
    }

    public void setFechaAlto(int fechaAlto) {
        this.fechaAlto = fechaAlto;
    }

    public int getFechaRadio() {
        return fechaRadio;
    }

    public void setFechaRadio(int fechaRadio) {
        this.fechaRadio = fechaRadio;
    }

    public int getFechaBordeGrosor() {
        return fechaBordeGrosor;
    }

    public void setFechaBordeGrosor(int fechaBordeGrosor) {
        this.fechaBordeGrosor = fechaBordeGrosor;
    }

    public String getFechaBordeColor() {
        return fechaBordeColor;
    }

    public void setFechaBordeColor(String fechaBordeColor) {
        this.fechaBordeColor = fechaBordeColor;
    }

    public boolean isFechaNegrita() {
        return fechaNegrita;
    }

    public void setFechaNegrita(boolean fechaNegrita) {
        this.fechaNegrita = fechaNegrita;
    }

    public String getFechaFamilia() {
        return fechaFamilia;
    }

    public void setFechaFamilia(String fechaFamilia) {
        this.fechaFamilia = fechaFamilia;
    }

    public String getFechaColor() {
        return fechaColor;
    }

    public void setFechaColor(String fechaColor) {
        this.fechaColor = fechaColor;
    }

    public int getFechaIconoTamano() {
        return fechaIconoTamano;
    }

    public void setFechaIconoTamano(int fechaIconoTamano) {
        this.fechaIconoTamano = fechaIconoTamano;
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
