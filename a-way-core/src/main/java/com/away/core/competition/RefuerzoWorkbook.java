package com.away.core.competition;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

final class RefuerzoWorkbook {

    private RefuerzoWorkbook() {
    }

    static List<EquipoLeido> leer(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            if (header == null) {
                return List.of();
            }
            Columnas columnas = ubicar(formatter, header);
            List<EquipoLeido> equipos = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                String nombre = texto(formatter, row, columnas.nombre);
                if (nombre.isEmpty()) {
                    continue;
                }
                EquipoLeido equipo = new EquipoLeido();
                equipo.fila = index + 1;
                equipo.nombre = limitar(nombre, 160);
                equipo.categoria = limitar(texto(formatter, row, columnas.categoria), 160);
                equipo.jugadores = jugadores(formatter, header, row, columnas.jugadores);
                equipos.add(equipo);
            }
            return equipos;
        }
    }

    private static Columnas ubicar(DataFormatter formatter, Row header) {
        int ultima = Math.max(header.getLastCellNum(), 0);
        int nombre = -1;
        int categoria = -1;
        for (int col = 0; col < ultima; col++) {
            String titulo = plano(texto(formatter, header, col));
            if (titulo.isEmpty()) {
                continue;
            }
            if (nombre < 0 && titulo.contains("nombre de equipo")) {
                nombre = col;
            } else if (categoria < 0 && titulo.contains("categoria")) {
                categoria = col;
            }
        }
        if (nombre < 0) {
            nombre = 2;
        }
        if (categoria < 0) {
            categoria = nombre + 1;
        }
        return new Columnas(nombre, categoria, categoria + 1);
    }

    private static List<JugadorLeido> jugadores(DataFormatter formatter, Row header, Row row, int inicio) {
        List<JugadorLeido> jugadores = new ArrayList<>();
        int col = inicio;
        int last = Math.max(header.getLastCellNum(), row.getLastCellNum());
        int orden = 1;
        while (col < last) {
            String titulo = texto(formatter, header, col);
            if (titulo.isEmpty()) {
                break;
            }
            String clave = plano(titulo);
            if (ignorar(clave)) {
                col++;
                continue;
            }
            if (!clave.contains("reemplaz")) {
                break;
            }
            String reemplazo = numero(formatter, row, col);
            String cedula = numero(formatter, row, col + 1);
            String nombre = texto(formatter, row, col + 2);
            String camisa = numero(formatter, row, col + 3);
            String foto = texto(formatter, row, col + 4);
            col += 5;
            if (cedula.isEmpty() && nombre.isEmpty()) {
                continue;
            }
            JugadorLeido jugador = new JugadorLeido();
            jugador.orden = orden++;
            jugador.numeroCamisaReemplazo = vacio(limitar(reemplazo, 40));
            jugador.cedula = vacio(limitar(cedula, 80));
            jugador.nombre = vacio(limitar(nombre, 200));
            jugador.numeroCamisa = vacio(limitar(camisa, 40));
            jugador.urlFoto = vacio(foto);
            jugadores.add(jugador);
        }
        return jugadores;
    }

    private static boolean ignorar(String titulo) {
        return titulo.contains("desea agregar")
                || titulo.contains("recibo")
                || titulo.contains("comprobante")
                || titulo.contains("adjuntar");
    }

    private static String plano(String titulo) {
        return Normalizer.normalize(titulo, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private static String texto(DataFormatter formatter, Row row, int column) {
        if (row == null || column < 0) {
            return "";
        }
        String value = formatter.formatCellValue(row.getCell(column));
        return value == null ? "" : value.trim();
    }

    private static String numero(DataFormatter formatter, Row row, int column) {
        return planoNumero(texto(formatter, row, column));
    }

    private static String planoNumero(String value) {
        String compact = value.replace(" ", "");
        if (compact.matches("(?i)[+-]?\\d+(\\.\\d+)?[eE][+-]?\\d+")) {
            try {
                String plain = new BigDecimal(compact).stripTrailingZeros().toPlainString();
                return plain.startsWith("+") ? plain.substring(1) : plain;
            } catch (NumberFormatException exception) {
                return value;
            }
        }
        if (compact.matches("\\d{1,3}([.,]\\d{3})+")) {
            return compact.replace(".", "").replace(",", "");
        }
        if (compact.matches("\\d+\\.0+")) {
            return compact.substring(0, compact.indexOf('.'));
        }
        return value;
    }

    private static String limitar(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String vacio(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    static final class EquipoLeido {
        int fila;
        String categoria;
        String nombre;
        List<JugadorLeido> jugadores;
    }

    static final class JugadorLeido {
        int orden;
        String numeroCamisaReemplazo;
        String cedula;
        String nombre;
        String numeroCamisa;
        String urlFoto;
    }

    private static final class Columnas {
        private final int nombre;
        private final int categoria;
        private final int jugadores;

        private Columnas(int nombre, int categoria, int jugadores) {
            this.nombre = nombre;
            this.categoria = categoria;
            this.jugadores = jugadores;
        }
    }
}
