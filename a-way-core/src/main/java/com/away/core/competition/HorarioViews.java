package com.away.core.competition;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class HorarioViews {

    private HorarioViews() {
    }

    public static class HorarioGuardarRequest {
        private UUID editionId;
        private UUID categoryId;
        private LocalDate fecha;
        private LocalDate dia;
        private List<Integer> jornadas;
        private UUID canchaId;
        private String hora;
        private String horaInicio;
        private Integer intervaloMinutos;

        public UUID getEditionId() {
            return editionId;
        }

        public void setEditionId(UUID editionId) {
            this.editionId = editionId;
        }

        public UUID getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(UUID categoryId) {
            this.categoryId = categoryId;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public void setFecha(LocalDate fecha) {
            this.fecha = fecha;
        }

        public LocalDate getDia() {
            return dia;
        }

        public void setDia(LocalDate dia) {
            this.dia = dia;
        }

        public List<Integer> getJornadas() {
            return jornadas;
        }

        public void setJornadas(List<Integer> jornadas) {
            this.jornadas = jornadas;
        }

        public UUID getCanchaId() {
            return canchaId;
        }

        public void setCanchaId(UUID canchaId) {
            this.canchaId = canchaId;
        }

        public String getHora() {
            return hora;
        }

        public void setHora(String hora) {
            this.hora = hora;
        }

        public String getHoraInicio() {
            return horaInicio;
        }

        public void setHoraInicio(String horaInicio) {
            this.horaInicio = horaInicio;
        }

        public Integer getIntervaloMinutos() {
            return intervaloMinutos;
        }

        public void setIntervaloMinutos(Integer intervaloMinutos) {
            this.intervaloMinutos = intervaloMinutos;
        }
    }

    public static class HorarioCanchaView {
        private final UUID id;
        private final String descripcion;

        public HorarioCanchaView(UUID id, String descripcion) {
            this.id = id;
            this.descripcion = descripcion;
        }

        public UUID getId() {
            return id;
        }

        public String getDescripcion() {
            return descripcion;
        }
    }

    public static class HorarioPartidoView {
        private final UUID id;
        private final UUID categoryId;
        private final String categoryName;
        private final int jornadaNumero;
        private final String jornadaNombre;
        private final int orden;
        private final LocalDate fecha;
        private final String hora;
        private final UUID canchaId;
        private final String canchaNombre;
        private final String textoLocal;
        private final String textoVisita;
        private final boolean equiposDefinidos;

        public HorarioPartidoView(UUID id, UUID categoryId, String categoryName, int jornadaNumero, String jornadaNombre,
                                   int orden, LocalDate fecha, String hora, UUID canchaId, String canchaNombre,
                                   String textoLocal, String textoVisita, boolean equiposDefinidos) {
            this.id = id;
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.jornadaNumero = jornadaNumero;
            this.jornadaNombre = jornadaNombre;
            this.orden = orden;
            this.fecha = fecha;
            this.hora = hora;
            this.canchaId = canchaId;
            this.canchaNombre = canchaNombre;
            this.textoLocal = textoLocal;
            this.textoVisita = textoVisita;
            this.equiposDefinidos = equiposDefinidos;
        }

        public UUID getId() {
            return id;
        }

        public UUID getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public int getJornadaNumero() {
            return jornadaNumero;
        }

        public String getJornadaNombre() {
            return jornadaNombre;
        }

        public int getOrden() {
            return orden;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public String getHora() {
            return hora;
        }

        public UUID getCanchaId() {
            return canchaId;
        }

        public String getCanchaNombre() {
            return canchaNombre;
        }

        public String getTextoLocal() {
            return textoLocal;
        }

        public String getTextoVisita() {
            return textoVisita;
        }

        public boolean isEquiposDefinidos() {
            return equiposDefinidos;
        }
    }

    public static class HorarioFechaView {
        private final LocalDate fecha;

        public HorarioFechaView(LocalDate fecha) {
            this.fecha = fecha;
        }

        public LocalDate getFecha() {
            return fecha;
        }
    }

    public static class HorarioPanelView {
        private final List<HorarioCanchaView> canchas;
        private final List<Integer> jornadas;
        private final List<HorarioPartidoView> partidos;
        private final String aviso;

        public HorarioPanelView(List<HorarioCanchaView> canchas, List<Integer> jornadas,
                                 List<HorarioPartidoView> partidos, String aviso) {
            this.canchas = canchas;
            this.jornadas = jornadas;
            this.partidos = partidos;
            this.aviso = aviso;
        }

        public List<HorarioCanchaView> getCanchas() {
            return canchas;
        }

        public List<Integer> getJornadas() {
            return jornadas;
        }

        public List<HorarioPartidoView> getPartidos() {
            return partidos;
        }

        public String getAviso() {
            return aviso;
        }
    }
}
