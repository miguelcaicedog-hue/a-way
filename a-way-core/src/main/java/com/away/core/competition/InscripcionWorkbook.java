package com.away.core.competition;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
                Columnas columnas = ubicar(formatter, header);
                String nombre = texto(formatter, row, columnas.nombre);
                if (nombre.isEmpty()) {
                    continue;
                }
                EquipoLeido equipo = new EquipoLeido();
                equipo.fila = index + 1;
                equipo.nombre = limitar(nombre, 160);
                equipo.logo = vacio(urlImagen(texto(formatter, row, columnas.logo)));
                equipo.categoria = limitar(texto(formatter, row, columnas.categoria), 160);
                equipo.representante = vacio(limitar(texto(formatter, row, columnas.representante), 160));
                equipo.telefono = vacio(limitar(texto(formatter, row, columnas.telefono), 80));
                equipo.jugadores = jugadores(formatter, header, row, columnas.jugadores);
                equipos.add(equipo);
            }
            return equipos;
        }
    }

    private static Columnas ubicar(DataFormatter formatter, Row header) {
        int ultima = Math.max(header.getLastCellNum(), 0);
        int nombre = -1;
        int logo = -1;
        int categoria = -1;
        int representante = -1;
        int telefono = -1;
        int jugadores = -1;
        for (int col = 0; col < ultima; col++) {
            String titulo = plano(texto(formatter, header, col));
            if (titulo.isEmpty()) {
                continue;
            }
            if (nombre < 0 && titulo.contains("nombre de equipo")) {
                nombre = col;
            } else if (logo < 0 && titulo.contains("logo") && !titulo.contains("jugador")) {
                logo = col;
            } else if (categoria < 0 && titulo.contains("categoria")) {
                categoria = col;
            } else if (representante < 0 && (titulo.contains("representante") || titulo.contains("capitan"))) {
                representante = col;
            } else if (telefono < 0 && titulo.contains("telefono")) {
                telefono = col;
            } else if (jugadores < 0 && cedula(titulo)) {
                jugadores = col;
            }
        }
        if (nombre < 0) {
            nombre = 3;
        }
        if (categoria < 0) {
            categoria = logo >= 0 ? logo + 1 : 4;
        }
        if (representante < 0) {
            representante = categoria + 1;
        }
        if (telefono < 0) {
            telefono = representante + 1;
        }
        if (jugadores < 0) {
            jugadores = telefono + 1;
        }
        return new Columnas(nombre, logo, categoria, representante, telefono, jugadores);
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
        return plano(titulo).contains("cedula");
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

    private static String limitar(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String urlImagen(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int inicio = value.indexOf("imgurl=");
        if (inicio < 0) {
            return value;
        }
        String encoded = value.substring(inicio + "imgurl=".length());
        int fin = encoded.indexOf('&');
        if (fin >= 0) {
            encoded = encoded.substring(0, fin);
        }
        String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        return decoded.startsWith("http://") || decoded.startsWith("https://") ? decoded : value;
    }

    private static String vacio(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    static final class EquipoLeido {
        int fila;
        String categoria;
        String nombre;
        String logo;
        String representante;
        String telefono;
        List<JugadorLeido> jugadores;
    }

    private static final class Columnas {
        private final int nombre;
        private final int logo;
        private final int categoria;
        private final int representante;
        private final int telefono;
        private final int jugadores;

        private Columnas(int nombre, int logo, int categoria, int representante, int telefono, int jugadores) {
            this.nombre = nombre;
            this.logo = logo;
            this.categoria = categoria;
            this.representante = representante;
            this.telefono = telefono;
            this.jugadores = jugadores;
        }
    }

    static final class JugadorLeido {
        int orden;
        String cedula;
        String nombre;
        String numeroCamisa;
        String urlFoto;
    }
}
