package com.away.core.auth;

public final class ClaveViews {

    private ClaveViews() {
    }

    public static class PoliticaView {
        private final int longitudMinima;
        private final boolean requiereLetra;
        private final boolean requiereNumero;
        private final boolean requiereEspecial;

        public PoliticaView(int longitudMinima, boolean requiereLetra, boolean requiereNumero, boolean requiereEspecial) {
            this.longitudMinima = longitudMinima;
            this.requiereLetra = requiereLetra;
            this.requiereNumero = requiereNumero;
            this.requiereEspecial = requiereEspecial;
        }

        public int getLongitudMinima() { return longitudMinima; }
        public boolean isRequiereLetra() { return requiereLetra; }
        public boolean isRequiereNumero() { return requiereNumero; }
        public boolean isRequiereEspecial() { return requiereEspecial; }
    }

    public static class CambioRequest {
        private String actual;
        private String nueva;

        public String getActual() { return actual; }
        public void setActual(String actual) { this.actual = actual; }
        public String getNueva() { return nueva; }
        public void setNueva(String nueva) { this.nueva = nueva; }
    }
}
