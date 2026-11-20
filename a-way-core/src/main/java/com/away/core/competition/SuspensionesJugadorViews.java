package com.away.core.competition;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SuspensionesJugadorViews {

    private SuspensionesJugadorViews() {
    }

    public static class PanelView {
        private final JornadaView jornadaProxima;
        private final List<JugadorView> jugadores;

        public PanelView(JornadaView jornadaProxima, List<JugadorView> jugadores) {
            this.jornadaProxima = jornadaProxima;
            this.jugadores = jugadores;
        }

        public JornadaView getJornadaProxima() {
            return jornadaProxima;
        }

        public List<JugadorView> getJugadores() {
            return jugadores;
        }
    }

    public static class JugadorView {
        private final UUID id;
        private final String alias;
        private final int numero;
        private final String equipo;
        private final boolean hasFoto;
        private final int rotacion;
        private final int pendientes;

        public JugadorView(UUID id, String alias, int numero, String equipo, boolean hasFoto, int rotacion, int pendientes) {
            this.id = id;
            this.alias = alias;
            this.numero = numero;
            this.equipo = equipo;
            this.hasFoto = hasFoto;
            this.rotacion = rotacion;
            this.pendientes = pendientes;
        }

        public UUID getId() {
            return id;
        }

        public String getAlias() {
            return alias;
        }

        public int getNumero() {
            return numero;
        }

        public String getEquipo() {
            return equipo;
        }

        public boolean isHasFoto() {
            return hasFoto;
        }

        public int getRotacion() {
            return rotacion;
        }

        public int getPendientes() {
            return pendientes;
        }
    }

    public static class DetalleView {
        private final UUID jugadorId;
        private final String alias;
        private final String equipo;
        private final List<OpcionView> opciones;
        private final List<JornadaView> jornadas;
        private final List<SuspensionView> suspensiones;

        public DetalleView(UUID jugadorId, String alias, String equipo, List<OpcionView> opciones,
                           List<JornadaView> jornadas, List<SuspensionView> suspensiones) {
            this.jugadorId = jugadorId;
            this.alias = alias;
            this.equipo = equipo;
            this.opciones = opciones;
            this.jornadas = jornadas;
            this.suspensiones = suspensiones;
        }

        public UUID getJugadorId() {
            return jugadorId;
        }

        public String getAlias() {
            return alias;
        }

        public String getEquipo() {
            return equipo;
        }

        public List<OpcionView> getOpciones() {
            return opciones;
        }

        public List<JornadaView> getJornadas() {
            return jornadas;
        }

        public List<SuspensionView> getSuspensiones() {
            return suspensiones;
        }
    }

    public static class OpcionView {
        private final String motivo;
        private final String etiqueta;
        private final int jornadas;
        private final boolean eligeInicio;

        public OpcionView(String motivo, String etiqueta, int jornadas, boolean eligeInicio) {
            this.motivo = motivo;
            this.etiqueta = etiqueta;
            this.jornadas = jornadas;
            this.eligeInicio = eligeInicio;
        }

        public String getMotivo() {
            return motivo;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public int getJornadas() {
            return jornadas;
        }

        public boolean isEligeInicio() {
            return eligeInicio;
        }
    }

    public static class JornadaView {
        private final UUID id;
        private final int numero;
        private final String nombre;
        private final LocalDate fecha;

        public JornadaView(UUID id, int numero, String nombre, LocalDate fecha) {
            this.id = id;
            this.numero = numero;
            this.nombre = nombre;
            this.fecha = fecha;
        }

        public UUID getId() {
            return id;
        }

        public int getNumero() {
            return numero;
        }

        public String getNombre() {
            return nombre;
        }

        public LocalDate getFecha() {
            return fecha;
        }
    }

    public static class SuspensionView {
        private final UUID id;
        private final String motivo;
        private final String etiqueta;
        private final String jornadaInicio;
        private final int jornadas;
        private final String origen;
        private final String observacion;

        public SuspensionView(UUID id, String motivo, String etiqueta, String jornadaInicio, int jornadas, String origen,
                              String observacion) {
            this.id = id;
            this.motivo = motivo;
            this.etiqueta = etiqueta;
            this.jornadaInicio = jornadaInicio;
            this.jornadas = jornadas;
            this.origen = origen;
            this.observacion = observacion;
        }

        public UUID getId() {
            return id;
        }

        public String getMotivo() {
            return motivo;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getJornadaInicio() {
            return jornadaInicio;
        }

        public int getJornadas() {
            return jornadas;
        }

        public String getOrigen() {
            return origen;
        }

        public String getObservacion() {
            return observacion;
        }
    }

    public static class AgregarRequest {
        private UUID jugadorId;
        private UUID categoryId;
        private String motivo;
        private Integer jornadas;
        private UUID jornadaInicioId;
        private String observacion;

        public UUID getJugadorId() {
            return jugadorId;
        }

        public void setJugadorId(UUID jugadorId) {
            this.jugadorId = jugadorId;
        }

        public UUID getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(UUID categoryId) {
            this.categoryId = categoryId;
        }

        public String getMotivo() {
            return motivo;
        }

        public void setMotivo(String motivo) {
            this.motivo = motivo;
        }

        public Integer getJornadas() {
            return jornadas;
        }

        public void setJornadas(Integer jornadas) {
            this.jornadas = jornadas;
        }

        public UUID getJornadaInicioId() {
            return jornadaInicioId;
        }

        public void setJornadaInicioId(UUID jornadaInicioId) {
            this.jornadaInicioId = jornadaInicioId;
        }

        public String getObservacion() {
            return observacion;
        }

        public void setObservacion(String observacion) {
            this.observacion = observacion;
        }
    }
}
