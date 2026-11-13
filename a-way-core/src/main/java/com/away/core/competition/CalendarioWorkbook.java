package com.away.core.competition;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CalendarioWorkbook {

    private static final DateTimeFormatter[] FECHAS = {
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    private CalendarioWorkbook() {
    }

    static List<PartidoLeido> leer(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(0);
            if (header == null) {
                return List.of();
            }
            Columnas columnas = ubicar(formatter, header);
            List<PartidoLeido> partidos = new ArrayList<>();
            Integer jornadaAnterior = null;
            LocalDate fechaAnterior = null;
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                String local = texto(formatter, row, columnas.local);
                String visita = texto(formatter, row, columnas.visita);
                Integer jornada = numero(formatter, row, columnas.jornada);
                LocalDate fecha = fecha(formatter, row, columnas.fecha);
                if (jornada != null) {
                    jornadaAnterior = jornada;
                } else {
                    jornada = jornadaAnterior;
                }
                if (fecha != null) {
                    fechaAnterior = fecha;
                } else {
                    fecha = fechaAnterior;
                }
                if (local.isEmpty() && visita.isEmpty()) {
                    continue;
                }
                if (local.isEmpty() || visita.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La fila " + (index + 1) + " no tiene local y visitante.");
                }
                if (jornada == null || jornada < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La fila " + (index + 1) + " no tiene un número de jornada.");
                }
                PartidoLeido partido = new PartidoLeido();
                partido.fila = index + 1;
                partido.jornada = jornada;
                partido.fecha = fecha;
                partido.local = local;
                partido.visita = visita;
                partidos.add(partido);
            }
            return partidos;
        }
    }

    private static Columnas ubicar(DataFormatter formatter, Row header) {
        int ultima = Math.max(header.getLastCellNum(), 0);
        int fecha = -1;
        int jornada = -1;
        int local = -1;
        int visita = -1;
        for (int col = 0; col < ultima; col++) {
            String titulo = plano(texto(formatter, header, col));
            if (titulo.isEmpty()) {
                continue;
            }
            if (fecha < 0 && titulo.contains("fecha")) {
                fecha = col;
            } else if (jornada < 0 && titulo.contains("jornada")) {
                jornada = col;
            } else if (local < 0 && titulo.contains("local")) {
                local = col;
            } else if (visita < 0 && (titulo.contains("visitante") || titulo.equals("visita"))) {
                visita = col;
            }
        }
        return new Columnas(fecha < 0 ? 0 : fecha, jornada < 0 ? 1 : jornada, local < 0 ? 2 : local,
                visita < 0 ? 3 : visita);
    }

    private static Integer numero(DataFormatter formatter, Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return (int) Math.round(cell.getNumericCellValue());
        }
        String value = texto(formatter, row, column);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.replace(',', '.')));
        } catch (NumberFormatException exception) {
            String digitos = value.replaceAll("[^0-9]", "");
            if (digitos.isEmpty()) {
                return null;
            }
            return Integer.parseInt(digitos);
        }
    }

    private static LocalDate fecha(DataFormatter formatter, Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double serial = cell.getNumericCellValue();
            if (DateUtil.isCellDateFormatted(cell) || (serial > 20000 && serial < 80000)) {
                return DateUtil.getLocalDateTime(serial).toLocalDate();
            }
            return null;
        }
        String value = texto(formatter, row, column);
        if (value.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formato : FECHAS) {
            try {
                return LocalDate.parse(value, formato);
            } catch (DateTimeParseException ignored) {
                // siguiente formato
            }
        }
        return null;
    }

    private static String texto(DataFormatter formatter, Row row, int column) {
        if (row == null || column < 0) {
            return "";
        }
        String value = formatter.formatCellValue(row.getCell(column));
        return value == null ? "" : value.trim();
    }

    static String clave(String nombre) {
        String plano = Normalizer.normalize(nombre == null ? "" : nombre.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
        return plano;
    }

    private static String plano(String titulo) {
        return clave(titulo).toLowerCase(Locale.ROOT);
    }

    static final class PartidoLeido {
        int fila;
        int jornada;
        LocalDate fecha;
        String local;
        String visita;
    }

    private static final class Columnas {
        private final int fecha;
        private final int jornada;
        private final int local;
        private final int visita;

        private Columnas(int fecha, int jornada, int local, int visita) {
            this.fecha = fecha;
            this.jornada = jornada;
            this.local = local;
            this.visita = visita;
        }
    }
}
