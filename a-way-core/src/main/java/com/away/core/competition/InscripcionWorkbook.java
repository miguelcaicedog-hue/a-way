package com.away.core.competition;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

final class InscripcionWorkbook {

    private InscripcionWorkbook() {
    }

    static List<EquipoLeido> leer(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            if (header == null) {
                return List.of();
            }
            List<EquipoLeido> equipos = new ArrayList<>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                String nombre = texto(formatter, row, 3);
                if (nombre.isEmpty()) {
                    continue;
                }
                EquipoLeido equipo = new EquipoLeido();
                equipo.fila = index + 1;
                equipo.nombre = limitar(nombre, 160);
                equipo.categoria = limitar(texto(formatter, row, 4), 160);
                equipo.representante = vacio(limitar(texto(formatter, row, 5), 160));
                equipo.telefono = vacio(limitar(texto(formatter, row, 6), 80));
                equipo.jugadores = jugadores(formatter, header, row);
                equipos.add(equipo);
            }
            return equipos;
        }
    }

    private static List<JugadorLeido> jugadores(DataFormatter formatter, Row header, Row row) {
        List<JugadorLeido> jugadores = new ArrayList<>();
        int col = 7;
        int last = Math.max(header.getLastCellNum(), row.getLastCellNum());
        int orden = 1;
        while (col < last) {
            String titulo = texto(formatter, header, col);
            if (titulo.isEmpty()) {
                break;
            }
            if (ignorar(titulo)) {
                col++;
                continue;
            }
            if (!cedula(titulo)) {
                break;
            }
            String documento = texto(formatter, row, col);
            String nombre = texto(formatter, row, col + 1);
            String camisa = texto(formatter, row, col + 2);
            String foto = texto(formatter, row, col + 3);
            col += 4;
            if (documento.isEmpty() && nombre.isEmpty()) {
                continue;
            }
            JugadorLeido jugador = new JugadorLeido();
            jugador.orden = orden++;
            jugador.cedula = vacio(limitar(documento, 80));
            jugador.nombre = vacio(limitar(nombre, 200));
            jugador.numeroCamisa = vacio(limitar(camisa, 40));
            jugador.urlFoto = vacio(foto);
            jugadores.add(jugador);
        }
        return jugadores;
    }

    private static boolean ignorar(String titulo) {
        return titulo.toLowerCase().contains("desea agregar");
    }

    private static boolean cedula(String titulo) {
        String plano = Normalizer.normalize(titulo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plano.toLowerCase().contains("cedula");
    }

    private static String texto(DataFormatter formatter, Row row, int column) {
        if (row == null || column < 0) {
            return "";
        }
        String value = formatter.formatCellValue(row.getCell(column));
        return value == null ? "" : value.trim();
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
        String representante;
        String telefono;
        List<JugadorLeido> jugadores;
    }

    static final class JugadorLeido {
        int orden;
        String cedula;
        String nombre;
        String numeroCamisa;
        String urlFoto;
    }
}
