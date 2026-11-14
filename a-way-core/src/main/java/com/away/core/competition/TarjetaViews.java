package com.away.core.competition;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class TarjetaViews {

    private TarjetaViews() {
    }

    public static class TarjetaRequest {
        @NotNull
        private UUID leagueId;
        private String descripcion;
        private String color;
        private BigDecimal costo;
        private Integer cantidadJornadas;
        private Integer pesoFairPlay;
        private String estado;

        public UUID getLeagueId() {
            return leagueId;
        }

        public void setLeagueId(UUID leagueId) {
            this.leagueId = leagueId;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public BigDecimal getCosto() {
            return costo;
        }

        public void setCosto(BigDecimal costo) {
            this.costo = costo;
        }

        public Integer getCantidadJornadas() {
            return cantidadJornadas;
        }

        public void setCantidadJornadas(Integer cantidadJornadas) {
            this.cantidadJornadas = cantidadJornadas;
        }

        public Integer getPesoFairPlay() {
            return pesoFairPlay;
        }

        public void setPesoFairPlay(Integer pesoFairPlay) {
            this.pesoFairPlay = pesoFairPlay;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    public static class TarjetaView {
        private final UUID id;
        private final UUID leagueId;
        private final String descripcion;
        private final String color;
        private final BigDecimal costo;
        private final int cantidadJornadas;
        private final int pesoFairPlay;
        private final String estado;

        TarjetaView(UUID id, UUID leagueId, String descripcion, String color, BigDecimal costo, int cantidadJornadas,
                    int pesoFairPlay, String estado) {
            this.id = id;
            this.leagueId = leagueId;
            this.descripcion = descripcion;
            this.color = color;
            this.costo = costo;
            this.cantidadJornadas = cantidadJornadas;
            this.pesoFairPlay = pesoFairPlay;
            this.estado = estado;
        }

        public UUID getId() {
            return id;
        }

        public UUID getLeagueId() {
            return leagueId;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getColor() {
            return color;
        }

        public BigDecimal getCosto() {
            return costo;
        }

        public int getCantidadJornadas() {
            return cantidadJornadas;
        }

        public int getPesoFairPlay() {
            return pesoFairPlay;
        }

        public String getEstado() {
            return estado;
        }
    }
}
