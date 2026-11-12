package com.away.core.competition;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TablaViews {

    private TablaViews() {
    }

    public static class TablaView {
        private final LocalDate fecha;
        private final List<GrupoView> grupos;

        public TablaView(LocalDate fecha, List<GrupoView> grupos) {
            this.fecha = fecha;
            this.grupos = grupos;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public List<GrupoView> getGrupos() {
            return grupos;
        }
    }

    public static class GrupoView {
        private final UUID id;
        private final String nombre;
        private final List<FilaView> equipos;

        public GrupoView(UUID id, String nombre, List<FilaView> equipos) {
            this.id = id;
            this.nombre = nombre;
            this.equipos = equipos;
        }

        public UUID getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public List<FilaView> getEquipos() {
            return equipos;
        }
    }

    public static class FilaView {
        private final int posicion;
        private final UUID teamId;
        private final String equipo;
        private final int pj;
        private final int ganados;
        private final int empatados;
        private final int perdidos;
        private final int puntos;
        private final int golesFavor;
        private final int golesContra;
        private final int diferencia;

        public FilaView(int posicion, UUID teamId, String equipo, int pj, int ganados, int empatados, int perdidos,
                        int puntos, int golesFavor, int golesContra, int diferencia) {
            this.posicion = posicion;
            this.teamId = teamId;
            this.equipo = equipo;
            this.pj = pj;
            this.ganados = ganados;
            this.empatados = empatados;
            this.perdidos = perdidos;
            this.puntos = puntos;
            this.golesFavor = golesFavor;
            this.golesContra = golesContra;
            this.diferencia = diferencia;
        }

        public int getPosicion() {
            return posicion;
        }

        public UUID getTeamId() {
            return teamId;
        }

        public String getEquipo() {
            return equipo;
        }

        public int getPj() {
            return pj;
        }

        public int getGanados() {
            return ganados;
        }

        public int getEmpatados() {
            return empatados;
        }

        public int getPerdidos() {
            return perdidos;
        }

        public int getPuntos() {
            return puntos;
        }

        public int getGolesFavor() {
            return golesFavor;
        }

        public int getGolesContra() {
            return golesContra;
        }

        public int getDiferencia() {
            return diferencia;
        }
    }
}
