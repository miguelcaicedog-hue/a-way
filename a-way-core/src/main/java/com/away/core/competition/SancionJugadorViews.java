package com.away.core.competition;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class SancionJugadorViews {

    private SancionJugadorViews() {
    }

    public static class SancionJugadorRequest {
        @NotNull
        private UUID leagueId;
        private String descripcion;
        private BigDecimal costo;
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

        public BigDecimal getCosto() {
            return costo;
        }

        public void setCosto(BigDecimal costo) {
            this.costo = costo;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    public static class SancionJugadorView {
        private final UUID id;
        private final UUID leagueId;
        private final String descripcion;
        private final BigDecimal costo;
        private final String estado;

        SancionJugadorView(UUID id, UUID leagueId, String descripcion, BigDecimal costo, String estado) {
            this.id = id;
            this.leagueId = leagueId;
            this.descripcion = descripcion;
            this.costo = costo;
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

        public BigDecimal getCosto() {
            return costo;
        }

        public String getEstado() {
            return estado;
        }
    }
}
