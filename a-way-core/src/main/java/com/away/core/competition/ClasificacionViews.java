package com.away.core.competition;

import java.time.LocalDate;
import java.util.List;

public class ClasificacionViews {

    private ClasificacionViews() {
    }

    public static class CuadroView {
        private final LocalDate fecha;
        private final List<RondaView> rondas;

        public CuadroView(LocalDate fecha, List<RondaView> rondas) {
            this.fecha = fecha;
            this.rondas = rondas;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public List<RondaView> getRondas() {
            return rondas;
        }
    }

    public static class RondaView {
        private final String nombre;
        private final int orden;
        private final String lado;
        private final List<LlaveView> llaves;

        public RondaView(String nombre, int orden, String lado, List<LlaveView> llaves) {
            this.nombre = nombre;
            this.orden = orden;
            this.lado = lado;
            this.llaves = llaves;
        }

        public String getNombre() {
            return nombre;
        }

        public int getOrden() {
            return orden;
        }

        public String getLado() {
            return lado;
        }

        public List<LlaveView> getLlaves() {
            return llaves;
        }
    }

    public static class LlaveView {
        private final String id;
        private final String local;
        private final String visita;
        private final boolean localEquipo;
        private final boolean visitaEquipo;
        private final Integer golesLocal;
        private final Integer golesVisita;

        public LlaveView(String id, String local, String visita, boolean localEquipo, boolean visitaEquipo,
                         Integer golesLocal, Integer golesVisita) {
            this.id = id;
            this.local = local;
            this.visita = visita;
            this.localEquipo = localEquipo;
            this.visitaEquipo = visitaEquipo;
            this.golesLocal = golesLocal;
            this.golesVisita = golesVisita;
        }

        public String getId() {
            return id;
        }

        public String getLocal() {
            return local;
        }

        public String getVisita() {
            return visita;
        }

        public boolean isLocalEquipo() {
            return localEquipo;
        }

        public boolean isVisitaEquipo() {
            return visitaEquipo;
        }

        public Integer getGolesLocal() {
            return golesLocal;
        }

        public Integer getGolesVisita() {
            return golesVisita;
        }
    }
}
