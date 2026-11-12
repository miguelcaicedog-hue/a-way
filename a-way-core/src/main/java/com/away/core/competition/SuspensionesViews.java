package com.away.core.competition;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

public class SuspensionesViews {

    private SuspensionesViews() {
    }

    public static class GuardarRequest {
        @NotNull
        private UUID leagueId;
        @NotBlank
        private String momento;
        @NotNull
        @Min(0)
        private Integer rojaJornadas;
        @NotNull
        @Min(1)
        private Integer amarillasAcumuladas;
        @NotNull
        @Min(0)
        private Integer amarillasJornadas;
        @NotNull
        @Min(1)
        private Integer rojasAcumuladas;
        @NotNull
        @Min(0)
        private Integer rojasJornadas;
        @NotNull
        @Min(0)
        private Integer faltaMediaJornadas;

        public UUID getLeagueId() {
            return leagueId;
        }

        public void setLeagueId(UUID leagueId) {
            this.leagueId = leagueId;
        }

        public String getMomento() {
            return momento;
        }

        public void setMomento(String momento) {
            this.momento = momento;
        }

        public Integer getRojaJornadas() {
            return rojaJornadas;
        }

        public void setRojaJornadas(Integer rojaJornadas) {
            this.rojaJornadas = rojaJornadas;
        }

        public Integer getAmarillasAcumuladas() {
            return amarillasAcumuladas;
        }

        public void setAmarillasAcumuladas(Integer amarillasAcumuladas) {
            this.amarillasAcumuladas = amarillasAcumuladas;
        }

        public Integer getAmarillasJornadas() {
            return amarillasJornadas;
        }

        public void setAmarillasJornadas(Integer amarillasJornadas) {
            this.amarillasJornadas = amarillasJornadas;
        }

        public Integer getRojasAcumuladas() {
            return rojasAcumuladas;
        }

        public void setRojasAcumuladas(Integer rojasAcumuladas) {
            this.rojasAcumuladas = rojasAcumuladas;
        }

        public Integer getRojasJornadas() {
            return rojasJornadas;
        }

        public void setRojasJornadas(Integer rojasJornadas) {
            this.rojasJornadas = rojasJornadas;
        }

        public Integer getFaltaMediaJornadas() {
            return faltaMediaJornadas;
        }

        public void setFaltaMediaJornadas(Integer faltaMediaJornadas) {
            this.faltaMediaJornadas = faltaMediaJornadas;
        }
    }

    public static class ReglaView {
        private final UUID leagueId;
        private final String momento;
        private final int rojaJornadas;
        private final int amarillasAcumuladas;
        private final int amarillasJornadas;
        private final int rojasAcumuladas;
        private final int rojasJornadas;
        private final int faltaMediaJornadas;
        private final String faltaGrave;

        public ReglaView(UUID leagueId, String momento, int rojaJornadas, int amarillasAcumuladas, int amarillasJornadas,
                         int rojasAcumuladas, int rojasJornadas, int faltaMediaJornadas) {
            this.leagueId = leagueId;
            this.momento = momento;
            this.rojaJornadas = rojaJornadas;
            this.amarillasAcumuladas = amarillasAcumuladas;
            this.amarillasJornadas = amarillasJornadas;
            this.rojasAcumuladas = rojasAcumuladas;
            this.rojasJornadas = rojasJornadas;
            this.faltaMediaJornadas = faltaMediaJornadas;
            this.faltaGrave = "ELIMINACION";
        }

        public UUID getLeagueId() {
            return leagueId;
        }

        public String getMomento() {
            return momento;
        }

        public int getRojaJornadas() {
            return rojaJornadas;
        }

        public int getAmarillasAcumuladas() {
            return amarillasAcumuladas;
        }

        public int getAmarillasJornadas() {
            return amarillasJornadas;
        }

        public int getRojasAcumuladas() {
            return rojasAcumuladas;
        }

        public int getRojasJornadas() {
            return rojasJornadas;
        }

        public int getFaltaMediaJornadas() {
            return faltaMediaJornadas;
        }

        public String getFaltaGrave() {
            return faltaGrave;
        }
    }
}
