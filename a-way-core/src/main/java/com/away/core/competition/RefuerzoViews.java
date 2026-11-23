package com.away.core.competition;

import java.util.List;
import java.util.UUID;

class RefuerzoArchivoView {
    private final UUID id;
    private final String nombreArchivo;
    private final String fechaCreacion;

    RefuerzoArchivoView(UUID id, String nombreArchivo, String fechaCreacion) {
        this.id = id;
        this.nombreArchivo = nombreArchivo;
        this.fechaCreacion = fechaCreacion;
    }

    public UUID getId() {
        return id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }
}

class RefuerzoEquipoView {
    private final UUID id;
    private final int numeroFila;
    private final String categoria;
    private final String nombreEquipo;
    private final boolean seleccionado;
    private final String resultado;
    private final String detalle;

    RefuerzoEquipoView(UUID id, int numeroFila, String categoria, String nombreEquipo,
                       boolean seleccionado, String resultado, String detalle) {
        this.id = id;
        this.numeroFila = numeroFila;
        this.categoria = categoria;
        this.nombreEquipo = nombreEquipo;
        this.seleccionado = seleccionado;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    public UUID getId() {
        return id;
    }

    public int getNumeroFila() {
        return numeroFila;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getNombreEquipo() {
        return nombreEquipo;
    }

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public String getResultado() {
        return resultado;
    }

    public String getDetalle() {
        return detalle;
    }
}

class RefuerzoDetalleView {
    private final UUID id;
    private final String nombreArchivo;
    private final List<RefuerzoEquipoView> equipos;

    RefuerzoDetalleView(UUID id, String nombreArchivo, List<RefuerzoEquipoView> equipos) {
        this.id = id;
        this.nombreArchivo = nombreArchivo;
        this.equipos = equipos;
    }

    public UUID getId() {
        return id;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public List<RefuerzoEquipoView> getEquipos() {
        return equipos;
    }
}

class RefuerzoJugadorView {
    private final UUID id;
    private final String cedula;
    private final String nombre;
    private final String alias;
    private final String numeroCamisa;
    private final String numeroCamisaReemplazo;
    private final boolean cargado;
    private final boolean existente;
    private final boolean fotoCargada;
    private final boolean tieneFoto;
    private final String motivo;

    RefuerzoJugadorView(UUID id, String cedula, String nombre, String alias, String numeroCamisa,
                        String numeroCamisaReemplazo, boolean cargado, boolean existente,
                        boolean fotoCargada, boolean tieneFoto, String motivo) {
        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.alias = alias;
        this.numeroCamisa = numeroCamisa;
        this.numeroCamisaReemplazo = numeroCamisaReemplazo;
        this.cargado = cargado;
        this.existente = existente;
        this.fotoCargada = fotoCargada;
        this.tieneFoto = tieneFoto;
        this.motivo = motivo;
    }

    public UUID getId() {
        return id;
    }

    public String getCedula() {
        return cedula;
    }

    public String getNombre() {
        return nombre;
    }

    public String getAlias() {
        return alias;
    }

    public String getNumeroCamisa() {
        return numeroCamisa;
    }

    public String getNumeroCamisaReemplazo() {
        return numeroCamisaReemplazo;
    }

    public boolean isCargado() {
        return cargado;
    }

    public boolean isExistente() {
        return existente;
    }

    public boolean isFotoCargada() {
        return fotoCargada;
    }

    public boolean isTieneFoto() {
        return tieneFoto;
    }

    public String getMotivo() {
        return motivo;
    }
}
