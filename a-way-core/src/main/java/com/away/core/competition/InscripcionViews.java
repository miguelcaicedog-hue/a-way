package com.away.core.competition;

import java.util.List;
import java.util.UUID;

class InscripcionArchivoView {
    private final UUID id;
    private final String nombreArchivo;
    private final String fechaCreacion;

    InscripcionArchivoView(UUID id, String nombreArchivo, String fechaCreacion) {
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

class InscripcionEquipoView {
    private final UUID id;
    private final int numeroFila;
    private final String categoria;
    private final String nombreEquipo;
    private final String nombreRepresentante;
    private final String numeroTelefono;
    private final boolean seleccionado;
    private final String resultado;
    private final String detalle;

    InscripcionEquipoView(UUID id, int numeroFila, String categoria, String nombreEquipo, String nombreRepresentante,
                          String numeroTelefono, boolean seleccionado, String resultado, String detalle) {
        this.id = id;
        this.numeroFila = numeroFila;
        this.categoria = categoria;
        this.nombreEquipo = nombreEquipo;
        this.nombreRepresentante = nombreRepresentante;
        this.numeroTelefono = numeroTelefono;
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

    public String getNombreRepresentante() {
        return nombreRepresentante;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
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

class InscripcionDetalleView {
    private final UUID id;
    private final String nombreArchivo;
    private final List<InscripcionEquipoView> equipos;

    InscripcionDetalleView(UUID id, String nombreArchivo, List<InscripcionEquipoView> equipos) {
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

    public List<InscripcionEquipoView> getEquipos() {
        return equipos;
    }
}

class InscripcionJugadorView {
    private final UUID id;
    private final String cedula;
    private final String nombre;
    private final String alias;
    private final String numeroCamisa;
    private final boolean cargado;
    private final boolean fotoCargada;
    private final boolean tieneFoto;
    private final String motivo;

    InscripcionJugadorView(UUID id, String cedula, String nombre, String alias, String numeroCamisa,
                           boolean cargado, boolean fotoCargada, boolean tieneFoto, String motivo) {
        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.alias = alias;
        this.numeroCamisa = numeroCamisa;
        this.cargado = cargado;
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

    public boolean isCargado() {
        return cargado;
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

class SeleccionRequest {
    private boolean seleccionado;

    public boolean isSeleccionado() {
        return seleccionado;
    }

    public void setSeleccionado(boolean seleccionado) {
        this.seleccionado = seleccionado;
    }
}

class AccesoFotosView {
    private final boolean acceso;
    private final String mensaje;

    AccesoFotosView(boolean acceso, String mensaje) {
        this.acceso = acceso;
        this.mensaje = mensaje;
    }

    public boolean isAcceso() {
        return acceso;
    }

    public String getMensaje() {
        return mensaje;
    }
}

class ProcesoRequest {
    private List<UUID> equipoIds;

    public List<UUID> getEquipoIds() {
        return equipoIds;
    }

    public void setEquipoIds(List<UUID> equipoIds) {
        this.equipoIds = equipoIds;
    }
}
