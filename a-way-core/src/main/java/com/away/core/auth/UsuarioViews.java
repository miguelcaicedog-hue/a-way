package com.away.core.auth;

import java.util.UUID;

public final class UsuarioViews {

    private UsuarioViews() {
    }

    public static class UsuarioView {
        private final UUID id;
        private final String username;
        private final String displayName;
        private final boolean master;
        private final String estado;
        private final String role;
        private final boolean hasFoto;

        public UsuarioView(UUID id, String username, String displayName, boolean master,
                           String estado, String role, boolean hasFoto) {
            this.id = id;
            this.username = username;
            this.displayName = displayName;
            this.master = master;
            this.estado = estado;
            this.role = role;
            this.hasFoto = hasFoto;
        }

        public UUID getId() { return id; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public boolean isMaster() { return master; }
        public String getEstado() { return estado; }
        public String getRole() { return role; }
        public boolean isHasFoto() { return hasFoto; }
    }

    public static class UsuarioRequest {
        private UUID leagueId;
        private String username;
        private String displayName;
        private String password;
        private String estado;
        private String role;
        private String fotoBase64;
        private String fotoContentType;
        private boolean clearFoto;

        public UUID getLeagueId() { return leagueId; }
        public void setLeagueId(UUID leagueId) { this.leagueId = leagueId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getFotoBase64() { return fotoBase64; }
        public void setFotoBase64(String fotoBase64) { this.fotoBase64 = fotoBase64; }
        public String getFotoContentType() { return fotoContentType; }
        public void setFotoContentType(String fotoContentType) { this.fotoContentType = fotoContentType; }
        public boolean isClearFoto() { return clearFoto; }
        public void setClearFoto(boolean clearFoto) { this.clearFoto = clearFoto; }
    }
}
