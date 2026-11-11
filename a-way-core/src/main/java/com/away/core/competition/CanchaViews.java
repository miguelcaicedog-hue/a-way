package com.away.core.competition;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

public class CanchaViews {

    private CanchaViews() {
    }

    public static class CanchaRequest {
        @NotNull
        private UUID leagueId;
        @NotBlank
        @Size(max = 160)
        private String descripcion;
        @NotBlank
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

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }
    }

    public static class CanchaView {
        private final UUID id;
        private final UUID leagueId;
        private final String descripcion;
        private final String estado;

        CanchaView(UUID id, UUID leagueId, String descripcion, String estado) {
            this.id = id;
            this.leagueId = leagueId;
            this.descripcion = descripcion;
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

        public String getEstado() {
            return estado;
        }
    }
}
