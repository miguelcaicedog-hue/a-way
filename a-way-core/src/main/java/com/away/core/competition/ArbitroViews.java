package com.away.core.competition;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

public class ArbitroViews {

    private ArbitroViews() {
    }

    public static class ArbitroRequest {
        @NotNull
        private UUID leagueId;
        @Size(max = 20)
        private String cedula;
        @Size(max = 160)
        private String nombre;
        @Size(max = 30)
        private String telefono;
        private String estado;
        private String fotoBase64;
        private String fotoContentType;
        private boolean clearFoto;

        public UUID getLeagueId() {
            return leagueId;
        }

        public void setLeagueId(UUID leagueId) {
            this.leagueId = leagueId;
        }

        public String getCedula() {
            return cedula;
        }

        public void setCedula(String cedula) {
            this.cedula = cedula;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public String getFotoBase64() {
            return fotoBase64;
        }

        public void setFotoBase64(String fotoBase64) {
            this.fotoBase64 = fotoBase64;
        }

        public String getFotoContentType() {
            return fotoContentType;
        }

        public void setFotoContentType(String fotoContentType) {
            this.fotoContentType = fotoContentType;
        }

        public boolean isClearFoto() {
            return clearFoto;
        }

        public void setClearFoto(boolean clearFoto) {
            this.clearFoto = clearFoto;
        }
    }

    public static class ArbitroView {
        private final UUID id;
        private final UUID leagueId;
        private final String cedula;
        private final String nombre;
        private final String telefono;
        private final boolean hasFoto;
        private final String estado;

        ArbitroView(UUID id, UUID leagueId, String cedula, String nombre, String telefono, boolean hasFoto, String estado) {
            this.id = id;
            this.leagueId = leagueId;
            this.cedula = cedula;
            this.nombre = nombre;
            this.telefono = telefono;
            this.hasFoto = hasFoto;
            this.estado = estado;
        }

        public UUID getId() {
            return id;
        }

        public UUID getLeagueId() {
            return leagueId;
        }

        public String getCedula() {
            return cedula;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTelefono() {
            return telefono;
        }

        public boolean isHasFoto() {
            return hasFoto;
        }

        public String getEstado() {
            return estado;
        }
    }
}
