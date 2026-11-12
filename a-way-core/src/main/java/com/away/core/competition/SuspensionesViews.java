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
        @NotBlank
        private String amarillasMomento;
        @NotNull
        @Min(1)
        private Integer rojasAcumuladas;
        @NotNull
        @Min(0)
        private Integer rojasJornadas;
        @NotBlank
        private String rojasMomento;
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

        public String getAmarillasMomento() {
            return amarillasMomento;
        }

        public void setAmarillasMomento(String amarillasMomento) {
            this.amarillasMomento = amarillasMomento;
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

        public String getRojasMomento() {
            return rojasMomento;
        }

        public void setRojasMomento(String rojasMomento) {
            this.rojasMomento = rojasMomento;
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
        private final String amarillasMomento;
        private final int rojasAcumuladas;
        private final int rojasJornadas;
        private final String rojasMomento;
        private final int faltaMediaJornadas;
        private final String faltaGrave;

        public ReglaView(UUID leagueId, String momento, int rojaJornadas, int amarillasAcumuladas, int amarillasJornadas,
                         String amarillasMomento, int rojasAcumuladas, int rojasJornadas, String rojasMomento,
                         int faltaMediaJornadas) {
            this.leagueId = leagueId;
            this.momento = momento;
            this.rojaJornadas = rojaJornadas;
            this.amarillasAcumuladas = amarillasAcumuladas;
            this.amarillasJornadas = amarillasJornadas;
            this.amarillasMomento = amarillasMomento;
            this.rojasAcumuladas = rojasAcumuladas;
            this.rojasJornadas = rojasJornadas;
            this.rojasMomento = rojasMomento;
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

        public String getAmarillasMomento() {
            return amarillasMomento;
        }

        public int getRojasAcumuladas() {
            return rojasAcumuladas;
        }

        public int getRojasJornadas() {
            return rojasJornadas;
        }

        public String getRojasMomento() {
            return rojasMomento;
        }

        public int getFaltaMediaJornadas() {
            return faltaMediaJornadas;
        }

        public String getFaltaGrave() {
            return faltaGrave;
        }
    }
}
