package com.away.core.competition;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

final class ReportePdf {

    private ReportePdf() {
    }

    static byte[] deHtml(String html) {
        return deHtml(html, 297, 210);
    }

    static byte[] deHtml(String html, float anchoMm, float altoMm) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useDefaultPageSize(anchoMm, altoMm, PdfRendererBuilder.PageSizeUnits.MM);
            builder.withHtmlContent(html, null);
            builder.toStream(salida);
            builder.run();
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el PDF", ex);
        }
    }
}
