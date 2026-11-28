package com.away.core.competition;

import java.util.List;
import java.util.UUID;

public class FormatoPublicacionViews {

    private FormatoPublicacionViews() {
    }

    public static class FormatoView {
        private final UUID id;
        private final UUID leagueId;
        private final String leagueName;
        private final String imagenPlantilla;
        private final boolean hasLogo;
        private final String nombreLugar;
        private final String descripcion;
        private final int logoX;
        private final int logoY;
        private final int logoAncho;
        private final int logoAlto;
        private final boolean logoHabilitado;
        private final boolean tituloHabilitado;
        private final boolean subtituloHabilitado;
        private final boolean lugarHabilitado;
        private final boolean fechaHabilitado;
        private final boolean tituloNegrita;
        private final boolean tituloCursiva;
        private final String tituloFamilia;
        private final String tituloColorInicio;
        private final String tituloColorFin;
        private final String tituloContornoColor;
        private final int tituloContornoGrosor;
        private final boolean tituloSombra;
        private final boolean subtituloNegrita;
        private final boolean subtituloCursiva;
        private final String subtituloFamilia;
        private final String subtituloColor;
        private final boolean lugarCursiva;
        private final String lugarFamilia;
        private final String lugarColor;
        private final int lugarRayaAncho;
        private final int lugarRayaGrosor;
        private final int fechaAncho;
        private final int fechaAlto;
        private final int fechaRadio;
        private final int fechaBordeGrosor;
        private final String fechaBordeColor;
        private final boolean fechaNegrita;
        private final String fechaFamilia;
        private final String fechaColor;
        private final int fechaIconoTamano;
        private final List<TablaView> tablas;

        FormatoView(FormatoPublicacion formato, String leagueName, String imagenPlantilla, boolean hasLogo,
                    List<TablaView> tablas) {
            this.id = formato.getId();
            this.leagueId = formato.getIdLiga();
            this.leagueName = leagueName;
            this.imagenPlantilla = imagenPlantilla;
            this.hasLogo = hasLogo;
            this.nombreLugar = formato.getNombreLugar();
            this.descripcion = formato.getDescripcion();
            this.logoX = formato.getLogoX();
            this.logoY = formato.getLogoY();
            this.logoAncho = formato.getLogoAncho();
            this.logoAlto = formato.getLogoAlto();
            this.logoHabilitado = formato.isLogoHabilitado();
            this.tituloHabilitado = formato.isTituloHabilitado();
            this.subtituloHabilitado = formato.isSubtituloHabilitado();
            this.lugarHabilitado = formato.isLugarHabilitado();
            this.fechaHabilitado = formato.isFechaHabilitado();
            this.tituloNegrita = formato.isTituloNegrita();
            this.tituloCursiva = formato.isTituloCursiva();
            this.tituloFamilia = formato.getTituloFamilia();
            this.tituloColorInicio = formato.getTituloColorInicio();
            this.tituloColorFin = formato.getTituloColorFin();
            this.tituloContornoColor = formato.getTituloContornoColor();
            this.tituloContornoGrosor = formato.getTituloContornoGrosor();
            this.tituloSombra = formato.isTituloSombra();
            this.subtituloNegrita = formato.isSubtituloNegrita();
            this.subtituloCursiva = formato.isSubtituloCursiva();
            this.subtituloFamilia = formato.getSubtituloFamilia();
            this.subtituloColor = formato.getSubtituloColor();
            this.lugarCursiva = formato.isLugarCursiva();
            this.lugarFamilia = formato.getLugarFamilia();
            this.lugarColor = formato.getLugarColor();
            this.lugarRayaAncho = formato.getLugarRayaAncho();
            this.lugarRayaGrosor = formato.getLugarRayaGrosor();
            this.fechaAncho = formato.getFechaAncho();
            this.fechaAlto = formato.getFechaAlto();
            this.fechaRadio = formato.getFechaRadio();
            this.fechaBordeGrosor = formato.getFechaBordeGrosor();
            this.fechaBordeColor = formato.getFechaBordeColor();
            this.fechaNegrita = formato.isFechaNegrita();
            this.fechaFamilia = formato.getFechaFamilia();
            this.fechaColor = formato.getFechaColor();
            this.fechaIconoTamano = formato.getFechaIconoTamano();
            this.tablas = tablas;
        }

        public UUID getId() { return id; }
        public UUID getLeagueId() { return leagueId; }
        public String getLeagueName() { return leagueName; }
        public String getImagenPlantilla() { return imagenPlantilla; }
        public boolean isHasLogo() { return hasLogo; }
        public String getNombreLugar() { return nombreLugar; }
        public String getDescripcion() { return descripcion; }
        public int getLogoX() { return logoX; }
        public int getLogoY() { return logoY; }
        public int getLogoAncho() { return logoAncho; }
        public int getLogoAlto() { return logoAlto; }
        public boolean isLogoHabilitado() { return logoHabilitado; }
        public boolean isTituloHabilitado() { return tituloHabilitado; }
        public boolean isSubtituloHabilitado() { return subtituloHabilitado; }
        public boolean isLugarHabilitado() { return lugarHabilitado; }
        public boolean isFechaHabilitado() { return fechaHabilitado; }
        public boolean isTituloNegrita() { return tituloNegrita; }
        public boolean isTituloCursiva() { return tituloCursiva; }
        public String getTituloFamilia() { return tituloFamilia; }
        public String getTituloColorInicio() { return tituloColorInicio; }
        public String getTituloColorFin() { return tituloColorFin; }
        public String getTituloContornoColor() { return tituloContornoColor; }
        public int getTituloContornoGrosor() { return tituloContornoGrosor; }
        public boolean isTituloSombra() { return tituloSombra; }
        public boolean isSubtituloNegrita() { return subtituloNegrita; }
        public boolean isSubtituloCursiva() { return subtituloCursiva; }
        public String getSubtituloFamilia() { return subtituloFamilia; }
        public String getSubtituloColor() { return subtituloColor; }
        public boolean isLugarCursiva() { return lugarCursiva; }
        public String getLugarFamilia() { return lugarFamilia; }
        public String getLugarColor() { return lugarColor; }
        public int getLugarRayaAncho() { return lugarRayaAncho; }
        public int getLugarRayaGrosor() { return lugarRayaGrosor; }
        public int getFechaAncho() { return fechaAncho; }
        public int getFechaAlto() { return fechaAlto; }
        public int getFechaRadio() { return fechaRadio; }
        public int getFechaBordeGrosor() { return fechaBordeGrosor; }
        public String getFechaBordeColor() { return fechaBordeColor; }
        public boolean isFechaNegrita() { return fechaNegrita; }
        public String getFechaFamilia() { return fechaFamilia; }
        public String getFechaColor() { return fechaColor; }
        public int getFechaIconoTamano() { return fechaIconoTamano; }
        public List<TablaView> getTablas() { return tablas; }
    }

    public static class TablaView {
        private final String tipo;
        private final int tablaX;
        private final int tablaY;
        private final int tablaAncho;
        private final int bordeGrosor;
        private final String bordeColor;
        private final int cabeceraAlto;
        private final String cabeceraFondo;
        private final String cabeceraColor;
        private final int cabeceraTamano;
        private final int filaAlto;
        private final String filaFondo;
        private final String filaColor;
        private final int filaTamano;
        private final String numeroColor;
        private final int numeroTamano;
        private final String destacadaFondo;
        private final String columnasTabla;
        private final String titulo;
        private final int tituloX;
        private final int tituloY;
        private final int tituloTamano;
        private final int subtituloX;
        private final int subtituloY;
        private final int subtituloTamano;
        private final int fechaX;
        private final int fechaY;
        private final int fechaTamano;
        private final int lugarY;
        private final int lugarTamano;
        private final boolean agruparPrimeraColumna;
        private final String grupoFamilia;
        private final int grupoTamano;
        private final int grupoAlto;
        private final String grupoFondo;
        private final String grupoColor;
        private final boolean ajustarAnchoTexto;
        private final String fondoPublicacion;

        TablaView(FormatoPublicacionTabla tabla, String imagenPlantilla) {
            this.tipo = tabla.getTipo();
            this.tablaX = tabla.getTablaX();
            this.tablaY = tabla.getTablaY();
            this.tablaAncho = tabla.getTablaAncho();
            this.bordeGrosor = tabla.getBordeGrosor();
            this.bordeColor = tabla.getBordeColor();
            this.cabeceraAlto = tabla.getCabeceraAlto();
            this.cabeceraFondo = tabla.getCabeceraFondo();
            this.cabeceraColor = tabla.getCabeceraColor();
            this.cabeceraTamano = tabla.getCabeceraTamano();
            this.filaAlto = tabla.getFilaAlto();
            this.filaFondo = tabla.getFilaFondo();
            this.filaColor = tabla.getFilaColor();
            this.filaTamano = tabla.getFilaTamano();
            this.numeroColor = tabla.getNumeroColor();
            this.numeroTamano = tabla.getNumeroTamano();
            this.destacadaFondo = tabla.getDestacadaFondo();
            this.columnasTabla = tabla.getColumnasTabla();
            this.titulo = tabla.getTitulo();
            this.tituloX = tabla.getTituloX();
            this.tituloY = tabla.getTituloY();
            this.tituloTamano = tabla.getTituloTamano();
            this.subtituloX = tabla.getSubtituloX();
            this.subtituloY = tabla.getSubtituloY();
            this.subtituloTamano = tabla.getSubtituloTamano();
            this.fechaX = tabla.getFechaX();
            this.fechaY = tabla.getFechaY();
            this.fechaTamano = tabla.getFechaTamano();
            this.lugarY = tabla.getLugarY();
            this.lugarTamano = tabla.getLugarTamano();
            this.agruparPrimeraColumna = tabla.isAgruparPrimeraColumna();
            this.grupoFamilia = tabla.getGrupoFamilia();
            this.grupoTamano = tabla.getGrupoTamano();
            this.grupoAlto = tabla.getGrupoAlto();
            this.grupoFondo = tabla.getGrupoFondo();
            this.grupoColor = tabla.getGrupoColor();
            this.ajustarAnchoTexto = tabla.isAjustarAnchoTexto();
            String fondo = tabla.getFondoPublicacion();
            this.fondoPublicacion = fondo == null || fondo.trim().isEmpty() ? imagenPlantilla : fondo;
        }

        public String getTipo() { return tipo; }
        public int getTablaX() { return tablaX; }
        public int getTablaY() { return tablaY; }
        public int getTablaAncho() { return tablaAncho; }
        public int getBordeGrosor() { return bordeGrosor; }
        public String getBordeColor() { return bordeColor; }
        public int getCabeceraAlto() { return cabeceraAlto; }
        public String getCabeceraFondo() { return cabeceraFondo; }
        public String getCabeceraColor() { return cabeceraColor; }
        public int getCabeceraTamano() { return cabeceraTamano; }
        public int getFilaAlto() { return filaAlto; }
        public String getFilaFondo() { return filaFondo; }
        public String getFilaColor() { return filaColor; }
        public int getFilaTamano() { return filaTamano; }
        public String getNumeroColor() { return numeroColor; }
        public int getNumeroTamano() { return numeroTamano; }
        public String getDestacadaFondo() { return destacadaFondo; }
        public String getColumnasTabla() { return columnasTabla; }
        public String getTitulo() { return titulo; }
        public int getTituloX() { return tituloX; }
        public int getTituloY() { return tituloY; }
        public int getTituloTamano() { return tituloTamano; }
        public int getSubtituloX() { return subtituloX; }
        public int getSubtituloY() { return subtituloY; }
        public int getSubtituloTamano() { return subtituloTamano; }
        public int getFechaX() { return fechaX; }
        public int getFechaY() { return fechaY; }
        public int getFechaTamano() { return fechaTamano; }
        public int getLugarY() { return lugarY; }
        public int getLugarTamano() { return lugarTamano; }
        public boolean isAgruparPrimeraColumna() { return agruparPrimeraColumna; }
        public String getGrupoFamilia() { return grupoFamilia; }
        public int getGrupoTamano() { return grupoTamano; }
        public int getGrupoAlto() { return grupoAlto; }
        public String getGrupoFondo() { return grupoFondo; }
        public String getGrupoColor() { return grupoColor; }
        public boolean isAjustarAnchoTexto() { return ajustarAnchoTexto; }
        public String getFondoPublicacion() { return fondoPublicacion; }
    }

    public static class FormatoRequest {
        private String nombreLugar;
        private String descripcion;
        private Integer logoX;
        private Integer logoY;
        private Integer logoAncho;
        private Integer logoAlto;
        private boolean logoHabilitado = true;
        private boolean tituloHabilitado = true;
        private boolean subtituloHabilitado = true;
        private boolean lugarHabilitado = true;
        private boolean fechaHabilitado = true;
        private boolean tituloNegrita;
        private boolean tituloCursiva;
        private String tituloFamilia;
        private String tituloColorInicio;
        private String tituloColorFin;
        private String tituloContornoColor;
        private Integer tituloContornoGrosor;
        private boolean tituloSombra;
        private boolean subtituloNegrita;
        private boolean subtituloCursiva;
        private String subtituloFamilia;
        private String subtituloColor;
        private boolean lugarCursiva;
        private String lugarFamilia;
        private String lugarColor;
        private Integer lugarRayaAncho;
        private Integer lugarRayaGrosor;
        private Integer fechaAncho;
        private Integer fechaAlto;
        private Integer fechaRadio;
        private Integer fechaBordeGrosor;
        private String fechaBordeColor;
        private boolean fechaNegrita;
        private String fechaFamilia;
        private String fechaColor;
        private Integer fechaIconoTamano;
        private List<TablaRequest> tablas;

        public String getNombreLugar() { return nombreLugar; }
        public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public Integer getLogoX() { return logoX; }
        public void setLogoX(Integer logoX) { this.logoX = logoX; }
        public Integer getLogoY() { return logoY; }
        public void setLogoY(Integer logoY) { this.logoY = logoY; }
        public Integer getLogoAncho() { return logoAncho; }
        public void setLogoAncho(Integer logoAncho) { this.logoAncho = logoAncho; }
        public Integer getLogoAlto() { return logoAlto; }
        public void setLogoAlto(Integer logoAlto) { this.logoAlto = logoAlto; }
        public boolean isLogoHabilitado() { return logoHabilitado; }
        public void setLogoHabilitado(boolean logoHabilitado) { this.logoHabilitado = logoHabilitado; }
        public boolean isTituloHabilitado() { return tituloHabilitado; }
        public void setTituloHabilitado(boolean tituloHabilitado) { this.tituloHabilitado = tituloHabilitado; }
        public boolean isSubtituloHabilitado() { return subtituloHabilitado; }
        public void setSubtituloHabilitado(boolean subtituloHabilitado) { this.subtituloHabilitado = subtituloHabilitado; }
        public boolean isLugarHabilitado() { return lugarHabilitado; }
        public void setLugarHabilitado(boolean lugarHabilitado) { this.lugarHabilitado = lugarHabilitado; }
        public boolean isFechaHabilitado() { return fechaHabilitado; }
        public void setFechaHabilitado(boolean fechaHabilitado) { this.fechaHabilitado = fechaHabilitado; }
        public boolean isTituloNegrita() { return tituloNegrita; }
        public void setTituloNegrita(boolean tituloNegrita) { this.tituloNegrita = tituloNegrita; }
        public boolean isTituloCursiva() { return tituloCursiva; }
        public void setTituloCursiva(boolean tituloCursiva) { this.tituloCursiva = tituloCursiva; }
        public String getTituloFamilia() { return tituloFamilia; }
        public void setTituloFamilia(String tituloFamilia) { this.tituloFamilia = tituloFamilia; }
        public String getTituloColorInicio() { return tituloColorInicio; }
        public void setTituloColorInicio(String tituloColorInicio) { this.tituloColorInicio = tituloColorInicio; }
        public String getTituloColorFin() { return tituloColorFin; }
        public void setTituloColorFin(String tituloColorFin) { this.tituloColorFin = tituloColorFin; }
        public String getTituloContornoColor() { return tituloContornoColor; }
        public void setTituloContornoColor(String tituloContornoColor) { this.tituloContornoColor = tituloContornoColor; }
        public Integer getTituloContornoGrosor() { return tituloContornoGrosor; }
        public void setTituloContornoGrosor(Integer tituloContornoGrosor) { this.tituloContornoGrosor = tituloContornoGrosor; }
        public boolean isTituloSombra() { return tituloSombra; }
        public void setTituloSombra(boolean tituloSombra) { this.tituloSombra = tituloSombra; }
        public boolean isSubtituloNegrita() { return subtituloNegrita; }
        public void setSubtituloNegrita(boolean subtituloNegrita) { this.subtituloNegrita = subtituloNegrita; }
        public boolean isSubtituloCursiva() { return subtituloCursiva; }
        public void setSubtituloCursiva(boolean subtituloCursiva) { this.subtituloCursiva = subtituloCursiva; }
        public String getSubtituloFamilia() { return subtituloFamilia; }
        public void setSubtituloFamilia(String subtituloFamilia) { this.subtituloFamilia = subtituloFamilia; }
        public String getSubtituloColor() { return subtituloColor; }
        public void setSubtituloColor(String subtituloColor) { this.subtituloColor = subtituloColor; }
        public boolean isLugarCursiva() { return lugarCursiva; }
        public void setLugarCursiva(boolean lugarCursiva) { this.lugarCursiva = lugarCursiva; }
        public String getLugarFamilia() { return lugarFamilia; }
        public void setLugarFamilia(String lugarFamilia) { this.lugarFamilia = lugarFamilia; }
        public String getLugarColor() { return lugarColor; }
        public void setLugarColor(String lugarColor) { this.lugarColor = lugarColor; }
        public Integer getLugarRayaAncho() { return lugarRayaAncho; }
        public void setLugarRayaAncho(Integer lugarRayaAncho) { this.lugarRayaAncho = lugarRayaAncho; }
        public Integer getLugarRayaGrosor() { return lugarRayaGrosor; }
        public void setLugarRayaGrosor(Integer lugarRayaGrosor) { this.lugarRayaGrosor = lugarRayaGrosor; }
        public Integer getFechaAncho() { return fechaAncho; }
        public void setFechaAncho(Integer fechaAncho) { this.fechaAncho = fechaAncho; }
        public Integer getFechaAlto() { return fechaAlto; }
        public void setFechaAlto(Integer fechaAlto) { this.fechaAlto = fechaAlto; }
        public Integer getFechaRadio() { return fechaRadio; }
        public void setFechaRadio(Integer fechaRadio) { this.fechaRadio = fechaRadio; }
        public Integer getFechaBordeGrosor() { return fechaBordeGrosor; }
        public void setFechaBordeGrosor(Integer fechaBordeGrosor) { this.fechaBordeGrosor = fechaBordeGrosor; }
        public String getFechaBordeColor() { return fechaBordeColor; }
        public void setFechaBordeColor(String fechaBordeColor) { this.fechaBordeColor = fechaBordeColor; }
        public boolean isFechaNegrita() { return fechaNegrita; }
        public void setFechaNegrita(boolean fechaNegrita) { this.fechaNegrita = fechaNegrita; }
        public String getFechaFamilia() { return fechaFamilia; }
        public void setFechaFamilia(String fechaFamilia) { this.fechaFamilia = fechaFamilia; }
        public String getFechaColor() { return fechaColor; }
        public void setFechaColor(String fechaColor) { this.fechaColor = fechaColor; }
        public Integer getFechaIconoTamano() { return fechaIconoTamano; }
        public void setFechaIconoTamano(Integer fechaIconoTamano) { this.fechaIconoTamano = fechaIconoTamano; }
        public List<TablaRequest> getTablas() { return tablas; }
        public void setTablas(List<TablaRequest> tablas) { this.tablas = tablas; }
    }

    public static class TablaRequest {
        private String tipo;
        private Integer tablaX;
        private Integer tablaY;
        private Integer tablaAncho;
        private Integer bordeGrosor;
        private String bordeColor;
        private Integer cabeceraAlto;
        private String cabeceraFondo;
        private String cabeceraColor;
        private Integer cabeceraTamano;
        private Integer filaAlto;
        private String filaFondo;
        private String filaColor;
        private Integer filaTamano;
        private String numeroColor;
        private Integer numeroTamano;
        private String destacadaFondo;
        private String columnasTabla;
        private String titulo;
        private Integer tituloX;
        private Integer tituloY;
        private Integer tituloTamano;
        private Integer subtituloX;
        private Integer subtituloY;
        private Integer subtituloTamano;
        private Integer fechaX;
        private Integer fechaY;
        private Integer fechaTamano;
        private Integer lugarY;
        private Integer lugarTamano;
        private boolean agruparPrimeraColumna;
        private String grupoFamilia;
        private Integer grupoTamano;
        private Integer grupoAlto;
        private String grupoFondo;
        private String grupoColor;
        private boolean ajustarAnchoTexto;
        private String fondoPublicacion;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public Integer getTablaX() { return tablaX; }
        public void setTablaX(Integer tablaX) { this.tablaX = tablaX; }
        public Integer getTablaY() { return tablaY; }
        public void setTablaY(Integer tablaY) { this.tablaY = tablaY; }
        public Integer getTablaAncho() { return tablaAncho; }
        public void setTablaAncho(Integer tablaAncho) { this.tablaAncho = tablaAncho; }
        public Integer getBordeGrosor() { return bordeGrosor; }
        public void setBordeGrosor(Integer bordeGrosor) { this.bordeGrosor = bordeGrosor; }
        public String getBordeColor() { return bordeColor; }
        public void setBordeColor(String bordeColor) { this.bordeColor = bordeColor; }
        public Integer getCabeceraAlto() { return cabeceraAlto; }
        public void setCabeceraAlto(Integer cabeceraAlto) { this.cabeceraAlto = cabeceraAlto; }
        public String getCabeceraFondo() { return cabeceraFondo; }
        public void setCabeceraFondo(String cabeceraFondo) { this.cabeceraFondo = cabeceraFondo; }
        public String getCabeceraColor() { return cabeceraColor; }
        public void setCabeceraColor(String cabeceraColor) { this.cabeceraColor = cabeceraColor; }
        public Integer getCabeceraTamano() { return cabeceraTamano; }
        public void setCabeceraTamano(Integer cabeceraTamano) { this.cabeceraTamano = cabeceraTamano; }
        public Integer getFilaAlto() { return filaAlto; }
        public void setFilaAlto(Integer filaAlto) { this.filaAlto = filaAlto; }
        public String getFilaFondo() { return filaFondo; }
        public void setFilaFondo(String filaFondo) { this.filaFondo = filaFondo; }
        public String getFilaColor() { return filaColor; }
        public void setFilaColor(String filaColor) { this.filaColor = filaColor; }
        public Integer getFilaTamano() { return filaTamano; }
        public void setFilaTamano(Integer filaTamano) { this.filaTamano = filaTamano; }
        public String getNumeroColor() { return numeroColor; }
        public void setNumeroColor(String numeroColor) { this.numeroColor = numeroColor; }
        public Integer getNumeroTamano() { return numeroTamano; }
        public void setNumeroTamano(Integer numeroTamano) { this.numeroTamano = numeroTamano; }
        public String getDestacadaFondo() { return destacadaFondo; }
        public void setDestacadaFondo(String destacadaFondo) { this.destacadaFondo = destacadaFondo; }
        public String getColumnasTabla() { return columnasTabla; }
        public void setColumnasTabla(String columnasTabla) { this.columnasTabla = columnasTabla; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public Integer getTituloX() { return tituloX; }
        public void setTituloX(Integer tituloX) { this.tituloX = tituloX; }
        public Integer getTituloY() { return tituloY; }
        public void setTituloY(Integer tituloY) { this.tituloY = tituloY; }
        public Integer getTituloTamano() { return tituloTamano; }
        public void setTituloTamano(Integer tituloTamano) { this.tituloTamano = tituloTamano; }
        public Integer getSubtituloX() { return subtituloX; }
        public void setSubtituloX(Integer subtituloX) { this.subtituloX = subtituloX; }
        public Integer getSubtituloY() { return subtituloY; }
        public void setSubtituloY(Integer subtituloY) { this.subtituloY = subtituloY; }
        public Integer getSubtituloTamano() { return subtituloTamano; }
        public void setSubtituloTamano(Integer subtituloTamano) { this.subtituloTamano = subtituloTamano; }
        public Integer getFechaX() { return fechaX; }
        public void setFechaX(Integer fechaX) { this.fechaX = fechaX; }
        public Integer getFechaY() { return fechaY; }
        public void setFechaY(Integer fechaY) { this.fechaY = fechaY; }
        public Integer getFechaTamano() { return fechaTamano; }
        public void setFechaTamano(Integer fechaTamano) { this.fechaTamano = fechaTamano; }
        public Integer getLugarY() { return lugarY; }
        public void setLugarY(Integer lugarY) { this.lugarY = lugarY; }
        public Integer getLugarTamano() { return lugarTamano; }
        public void setLugarTamano(Integer lugarTamano) { this.lugarTamano = lugarTamano; }
        public boolean isAgruparPrimeraColumna() { return agruparPrimeraColumna; }
        public void setAgruparPrimeraColumna(boolean agruparPrimeraColumna) { this.agruparPrimeraColumna = agruparPrimeraColumna; }
        public String getGrupoFamilia() { return grupoFamilia; }
        public void setGrupoFamilia(String grupoFamilia) { this.grupoFamilia = grupoFamilia; }
        public Integer getGrupoTamano() { return grupoTamano; }
        public void setGrupoTamano(Integer grupoTamano) { this.grupoTamano = grupoTamano; }
        public Integer getGrupoAlto() { return grupoAlto; }
        public void setGrupoAlto(Integer grupoAlto) { this.grupoAlto = grupoAlto; }
        public String getGrupoFondo() { return grupoFondo; }
        public void setGrupoFondo(String grupoFondo) { this.grupoFondo = grupoFondo; }
        public String getGrupoColor() { return grupoColor; }
        public void setGrupoColor(String grupoColor) { this.grupoColor = grupoColor; }
        public boolean isAjustarAnchoTexto() { return ajustarAnchoTexto; }
        public void setAjustarAnchoTexto(boolean ajustarAnchoTexto) { this.ajustarAnchoTexto = ajustarAnchoTexto; }
        public String getFondoPublicacion() { return fondoPublicacion; }
        public void setFondoPublicacion(String fondoPublicacion) { this.fondoPublicacion = fondoPublicacion; }
    }
}
