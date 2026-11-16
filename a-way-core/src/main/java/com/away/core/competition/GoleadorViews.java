package com.away.core.competition;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class GoleadorViews {

    private GoleadorViews() {
    }

    public static class PanelView {
        private final LocalDate fecha;
        private final List<EquipoView> equipos;
        private final List<GoleadorView> goleadores;

        public PanelView(LocalDate fecha, List<EquipoView> equipos, List<GoleadorView> goleadores) {
            this.fecha = fecha;
            this.equipos = equipos;
            this.goleadores = goleadores;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public List<EquipoView> getEquipos() {
            return equipos;
        }

        public List<GoleadorView> getGoleadores() {
            return goleadores;
        }
    }

    public static class EquipoView {
        private final UUID id;
        private final String nombre;

        public EquipoView(UUID id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public UUID getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }
    }

    public static class GoleadorView {
        private final UUID jugadorId;
        private final String alias;
        private final String nombre;
        private final int numeroCamisa;
        private final UUID equipoId;
        private final String equipo;
        private final int goles;
        private final boolean hasPhoto;
        private final int rotationDegrees;
        private final List<DetalleView> detalle;

        public GoleadorView(UUID jugadorId, String alias, String nombre, int numeroCamisa, UUID equipoId, String equipo, int goles,
                            boolean hasPhoto, int rotationDegrees, List<DetalleView> detalle) {
            this.jugadorId = jugadorId;
            this.alias = alias;
            this.nombre = nombre;
            this.numeroCamisa = numeroCamisa;
            this.equipoId = equipoId;
            this.equipo = equipo;
            this.goles = goles;
            this.hasPhoto = hasPhoto;
            this.rotationDegrees = rotationDegrees;
            this.detalle = detalle;
        }

        public UUID getJugadorId() {
            return jugadorId;
        }

        public String getAlias() {
            return alias;
        }

        public String getNombre() {
            return nombre;
        }

        public int getNumeroCamisa() {
            return numeroCamisa;
        }

        public UUID getEquipoId() {
            return equipoId;
        }

        public String getEquipo() {
            return equipo;
        }

        public int getGoles() {
            return goles;
        }

        public boolean isHasPhoto() {
            return hasPhoto;
        }

        public int getRotationDegrees() {
            return rotationDegrees;
        }

        public List<DetalleView> getDetalle() {
            return detalle;
        }
    }

    public static class DetalleView {
        private final String jornada;
        private final LocalDate fecha;
        private final String rival;
        private final int cantidad;

        public DetalleView(String jornada, LocalDate fecha, String rival, int cantidad) {
            this.jornada = jornada;
            this.fecha = fecha;
            this.rival = rival;
            this.cantidad = cantidad;
        }

        public String getJornada() {
            return jornada;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public String getRival() {
            return rival;
        }

        public int getCantidad() {
            return cantidad;
        }
    }
}
