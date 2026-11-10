package com.away.core.competition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FotoInscripcion {

    private static final Pattern DRIVE_ID = Pattern.compile("(?:[?&]id=|/file/d/)([A-Za-z0-9_-]+)");
    private static final Pattern CONFIRM = Pattern.compile("confirm=([0-9A-Za-z_-]+)");
    private static final int MAX_BYTES = 2_000_000;

    private FotoInscripcion() {
    }

    static Descarga descargar(String celda) {
        String url = ultimaUrl(celda);
        if (url == null) {
            return Descarga.error("No hay una URL de foto");
        }
        String destino = urlDeDescarga(url);
        CookieManager cookies = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        try {
            Respuesta respuesta = pedir(client, destino);
            if (esImagen(respuesta)) {
                return validar(respuesta);
            }
            String confirm = confirmacion(respuesta.cuerpo);
            if (confirm == null) {
                return Descarga.error("No se pudo descargar la foto");
            }
            String repetir = destino + (destino.contains("?") ? "&" : "?") + "confirm=" + confirm;
            Respuesta segunda = pedir(client, repetir);
            if (!esImagen(segunda)) {
                return Descarga.error("No se pudo descargar la foto");
            }
            return validar(segunda);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Descarga.error("No se pudo descargar la foto");
        }
    }

    private static Descarga validar(Respuesta respuesta) {
        if (respuesta.cuerpo.length == 0) {
            return Descarga.error("No se pudo descargar la foto");
        }
        if (respuesta.cuerpo.length > 1_500_000) {
            return Descarga.error("La foto debe pesar menos de 1.5 MB");
        }
        String tipo = tipo(respuesta);
        if (tipo == null) {
            return Descarga.error("La foto debe ser JPEG, PNG, WEBP o GIF");
        }
        return Descarga.ok(respuesta.cuerpo, tipo);
    }

    private static Respuesta pedir(HttpClient client, String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream input = response.body()) {
            return new Respuesta(leer(input), response.headers().firstValue("content-type").orElse(""));
        }
    }

    private static byte[] leer(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_BYTES) {
                throw new IOException("foto demasiado grande");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String ultimaUrl(String celda) {
        if (celda == null || celda.isBlank()) {
            return null;
        }
        String ultima = null;
        for (String parte : celda.split(",")) {
            String valor = parte.trim();
            if (valor.startsWith("http://") || valor.startsWith("https://")) {
                ultima = valor;
            }
        }
        return ultima;
    }

    private static String urlDeDescarga(String url) {
        Matcher matcher = DRIVE_ID.matcher(url);
        if (!matcher.find()) {
            return url;
        }
        return "https://drive.google.com/uc?export=download&id=" + matcher.group(1);
    }

    private static String confirmacion(byte[] cuerpo) {
        String html = new String(cuerpo, java.nio.charset.StandardCharsets.UTF_8);
        Matcher matcher = CONFIRM.matcher(html);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private static boolean esImagen(Respuesta respuesta) {
        return tipo(respuesta) != null;
    }

    private static String tipo(Respuesta respuesta) {
        String declarado = respuesta.tipo == null ? "" : respuesta.tipo.toLowerCase();
        if (declarado.contains("jpeg") || declarado.contains("jpg") || declarado.contains("pjpeg")) {
            return "image/jpeg";
        }
        if (declarado.contains("png")) {
            return "image/png";
        }
        if (declarado.contains("webp")) {
            return "image/webp";
        }
        if (declarado.contains("gif")) {
            return "image/gif";
        }
        byte[] cuerpo = respuesta.cuerpo;
        if (cuerpo.length >= 3 && (cuerpo[0] & 0xFF) == 0xFF && (cuerpo[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (cuerpo.length >= 8 && cuerpo[0] == (byte) 0x89 && cuerpo[1] == 0x50 && cuerpo[2] == 0x4E && cuerpo[3] == 0x47) {
            return "image/png";
        }
        if (cuerpo.length >= 6 && cuerpo[0] == 'G' && cuerpo[1] == 'I' && cuerpo[2] == 'F') {
            return "image/gif";
        }
        if (cuerpo.length >= 12 && cuerpo[0] == 'R' && cuerpo[1] == 'I' && cuerpo[8] == 'W' && cuerpo[9] == 'E') {
            return "image/webp";
        }
        return null;
    }

    static final class Descarga {
        final byte[] bytes;
        final String contentType;
        final String error;

        private Descarga(byte[] bytes, String contentType, String error) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.error = error;
        }

        static Descarga ok(byte[] bytes, String contentType) {
            return new Descarga(bytes, contentType, null);
        }

        static Descarga error(String error) {
            return new Descarga(null, null, error);
        }
    }

    private static final class Respuesta {
        final byte[] cuerpo;
        final String tipo;

        private Respuesta(byte[] cuerpo, String tipo) {
            this.cuerpo = cuerpo;
            this.tipo = tipo;
        }
    }
}
