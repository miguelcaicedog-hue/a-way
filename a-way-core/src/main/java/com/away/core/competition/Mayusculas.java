package com.away.core.competition;

import java.util.Locale;

final class Mayusculas {

    private static final Locale ESPANOL = Locale.forLanguageTag("es");

    private Mayusculas() {
    }

    static String de(String valor) {
        return valor == null ? null : valor.toUpperCase(ESPANOL);
    }
}
