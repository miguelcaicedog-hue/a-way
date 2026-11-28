package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FormatoPublicacionService {

    static final List<String> TIPOS = Arrays.asList("GOLEADORES", "RESULTADOS", "POSICIONES", "HORARIOS");
    static final List<String> COLUMNAS_POSICIONES = Arrays.asList(
            "POSICION", "EQUIPO", "PJ", "G", "E", "P", "PTS", "GF", "GC", "DIF");
    static final String COLUMNAS_INICIALES = "POSICION,EQUIPO,PJ,PTS,DIF";

    private final FormatoPublicacionRepository formatos;
    private final FormatoPublicacionTablaRepository tablas;
    private final LeagueRepository leagues;
    private final AppUserRepository users;

    public FormatoPublicacionService(FormatoPublicacionRepository formatos, FormatoPublicacionTablaRepository tablas,
                                     LeagueRepository leagues, AppUserRepository users) {
        this.formatos = formatos;
        this.tablas = tablas;
        this.leagues = leagues;
        this.users = users;
    }

    @Transactional
    public void crear(UUID leagueId) {
        if (formatos.findByIdLigaAndEstado(leagueId, "ACTIVO").isPresent()) {
            return;
        }
        String usuario = usuario();
        FormatoPublicacion formato = defecto(liga(leagueId), usuario);
        formatos.save(formato);
        for (String tipo : TIPOS) {
            tablas.save(tablaDefecto(formato, tipo, usuario));
        }
    }

    @Transactional
    public void eliminar(UUID leagueId) {
        tablas.deleteByIdLiga(leagueId);
        formatos.deleteByIdLiga(leagueId);
        formatos.flush();
    }

    @Transactional(readOnly = true)
    public FormatoPublicacionViews.FormatoView obtener(UUID leagueId) {
        assertRead(leagueId);
        return vista(liga(leagueId));
    }

    @Transactional
    public FormatoPublicacionViews.FormatoView guardar(UUID leagueId, FormatoPublicacionViews.FormatoRequest request) {
        assertWrite(leagueId);
        League league = liga(leagueId);
        FormatoPublicacion formato = formatos.findByIdLigaAndEstado(leagueId, "ACTIVO")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no tiene formato de publicación"));
        aplicar(formato, request);
        auditar(formato, usuario());
        aplicarTablas(formato, request.getTablas(), league.getImagenPlantilla(), usuario());
        return vista(league);
    }

    private FormatoPublicacionViews.FormatoView vista(League league) {
        FormatoPublicacion formato = formatos.findByIdLigaAndEstado(league.getId(), "ACTIVO")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no tiene formato de publicación"));
        List<FormatoPublicacionTabla> filas = tablas.findByIdLigaAndEstado(league.getId(), "ACTIVO");
        List<FormatoPublicacionViews.TablaView> vistas = new ArrayList<>();
        for (String tipo : TIPOS) {
            FormatoPublicacionTabla fila = filas.stream()
                    .filter(item -> tipo.equals(item.getTipo()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Falta la tabla " + tipo));
            vistas.add(new FormatoPublicacionViews.TablaView(fila, league.getImagenPlantilla()));
        }
        byte[] logo = league.getLogo();
        return new FormatoPublicacionViews.FormatoView(
                formato, league.getName(), league.getImagenPlantilla(), logo != null && logo.length > 0, vistas);
    }

    private void aplicar(FormatoPublicacion formato, FormatoPublicacionViews.FormatoRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El formato es obligatorio");
        }
        String lugar = request.getNombreLugar() == null ? "" : request.getNombreLugar().trim();
        if (lugar.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de lugar admite hasta 80 caracteres");
        }
        formato.setNombreLugar(lugar);
        formato.setDescripcion(texto(request.getDescripcion(), "La descripción", 120, true));
        formato.setLogoX(medida(request.getLogoX(), "logo x", 0, 4000));
        formato.setLogoY(medida(request.getLogoY(), "logo y", 0, 4000));
        formato.setLogoAncho(medida(request.getLogoAncho(), "logo ancho", 1, 4000));
        formato.setLogoAlto(medida(request.getLogoAlto(), "logo alto", 1, 4000));
        formato.setLogoHabilitado(request.isLogoHabilitado());
        formato.setTituloHabilitado(request.isTituloHabilitado());
        formato.setSubtituloHabilitado(request.isSubtituloHabilitado());
        formato.setLugarHabilitado(request.isLugarHabilitado());
        formato.setFechaHabilitado(request.isFechaHabilitado());
        formato.setTituloNegrita(request.isTituloNegrita());
        formato.setTituloCursiva(request.isTituloCursiva());
        formato.setTituloFamilia(familia(request.getTituloFamilia(), "título"));
        formato.setTituloColorInicio(color(request.getTituloColorInicio(), "título"));
        formato.setTituloColorFin(color(request.getTituloColorFin(), "degradado del título"));
        formato.setTituloContornoColor(color(request.getTituloContornoColor(), "contorno del título"));
        formato.setTituloContornoGrosor(medida(request.getTituloContornoGrosor(), "contorno del título", 0, 20));
        formato.setTituloSombra(request.isTituloSombra());
        formato.setSubtituloNegrita(request.isSubtituloNegrita());
        formato.setSubtituloCursiva(request.isSubtituloCursiva());
        formato.setSubtituloFamilia(familia(request.getSubtituloFamilia(), "subtítulo"));
        formato.setSubtituloColor(color(request.getSubtituloColor(), "subtítulo"));
        formato.setLugarCursiva(request.isLugarCursiva());
        formato.setLugarFamilia(familia(request.getLugarFamilia(), "nombre de lugar"));
        formato.setLugarColor(color(request.getLugarColor(), "nombre de lugar"));
        formato.setLugarRayaAncho(medida(request.getLugarRayaAncho(), "raya del lugar", 0, 1000));
        formato.setLugarRayaGrosor(medida(request.getLugarRayaGrosor(), "grosor de la raya", 0, 20));
        formato.setFechaAncho(medida(request.getFechaAncho(), "fecha ancho", 1, 4000));
        formato.setFechaAlto(medida(request.getFechaAlto(), "fecha alto", 1, 400));
        formato.setFechaRadio(medida(request.getFechaRadio(), "fecha radio", 0, 200));
        formato.setFechaBordeGrosor(medida(request.getFechaBordeGrosor(), "borde de la fecha", 0, 20));
        formato.setFechaBordeColor(color(request.getFechaBordeColor(), "borde de la fecha"));
        formato.setFechaNegrita(request.isFechaNegrita());
        formato.setFechaFamilia(familia(request.getFechaFamilia(), "fecha"));
        formato.setFechaColor(color(request.getFechaColor(), "fecha"));
        formato.setFechaIconoTamano(medida(request.getFechaIconoTamano(), "icono de la fecha", 8, 128));
    }

    private void aplicarTablas(FormatoPublicacion formato, List<FormatoPublicacionViews.TablaRequest> pedidos,
                               String imagenPlantilla, String usuario) {
        if (pedidos == null || pedidos.size() != TIPOS.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar el estilo de goleadores, resultados, posiciones y horarios");
        }
        List<FormatoPublicacionTabla> filas = tablas.findByIdLigaAndEstado(formato.getIdLiga(), "ACTIVO");
        for (String tipo : TIPOS) {
            FormatoPublicacionViews.TablaRequest pedido = pedidos.stream()
                    .filter(item -> tipo.equals(item.getTipo()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el estilo de " + tipo));
            FormatoPublicacionTabla fila = filas.stream()
                    .filter(item -> tipo.equals(item.getTipo()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Falta la tabla " + tipo));
            fila.setTablaX(medida(pedido.getTablaX(), "tabla x", 0, 4000));
            fila.setTablaY(medida(pedido.getTablaY(), "tabla y", 0, 4000));
            fila.setTablaAncho(medida(pedido.getTablaAncho(), "tabla ancho", 1, 4000));
            fila.setBordeGrosor(medida(pedido.getBordeGrosor(), "borde de la tabla", 0, 20));
            fila.setBordeColor(color(pedido.getBordeColor(), "borde de la tabla"));
            fila.setCabeceraAlto(medida(pedido.getCabeceraAlto(), "cabecera", 8, 200));
            fila.setCabeceraFondo(color(pedido.getCabeceraFondo(), "cabecera"));
            fila.setCabeceraColor(color(pedido.getCabeceraColor(), "texto de cabecera"));
            fila.setCabeceraTamano(medida(pedido.getCabeceraTamano(), "texto de cabecera", 8, 80));
            fila.setFilaAlto(medida(pedido.getFilaAlto(), "fila", 8, 200));
            fila.setFilaFondo(color(pedido.getFilaFondo(), "fila"));
            fila.setFilaColor(color(pedido.getFilaColor(), "texto de fila"));
            fila.setFilaTamano(medida(pedido.getFilaTamano(), "texto de fila", 8, 80));
            fila.setNumeroColor(color(pedido.getNumeroColor(), "número"));
            fila.setNumeroTamano(medida(pedido.getNumeroTamano(), "número", 8, 120));
            fila.setDestacadaFondo(color(pedido.getDestacadaFondo(), "columna destacada"));
            fila.setColumnasTabla("POSICIONES".equals(tipo) ? columnas(pedido.getColumnasTabla()) : "");
            fila.setTitulo(texto(pedido.getTitulo(), "El título de " + tipo.toLowerCase(Locale.ROOT), 80, false));
            fila.setTituloX(medida(pedido.getTituloX(), "título x", 0, 4000));
            fila.setTituloY(medida(pedido.getTituloY(), "título y", 0, 4000));
            fila.setTituloTamano(medida(pedido.getTituloTamano(), "título tamaño", 8, 200));
            fila.setSubtituloX(medida(pedido.getSubtituloX(), "subtítulo x", 0, 4000));
            fila.setSubtituloY(medida(pedido.getSubtituloY(), "subtítulo y", 0, 4000));
            fila.setSubtituloTamano(medida(pedido.getSubtituloTamano(), "subtítulo tamaño", 8, 200));
            fila.setFechaX(medida(pedido.getFechaX(), "fecha x", 0, 4000));
            fila.setFechaY(medida(pedido.getFechaY(), "fecha y", 0, 4000));
            fila.setFechaTamano(medida(pedido.getFechaTamano(), "fecha tamaño", 8, 120));
            fila.setLugarY(medida(pedido.getLugarY(), "nombre de lugar y", 0, 4000));
            fila.setLugarTamano(medida(pedido.getLugarTamano(), "nombre de lugar tamaño", 8, 200));
            fila.setAgruparPrimeraColumna(pedido.isAgruparPrimeraColumna());
            String fondo = fondoPublicacion(pedido.getFondoPublicacion());
            fila.setFondoPublicacion(fondo != null && fondo.equals(imagenPlantilla) ? null : fondo);
            fila.setFechaModificacion(OffsetDateTime.now());
            fila.setUsuarioModificacion(usuario);
        }
    }

    private String columnas(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marca al menos una columna de la tabla de posiciones");
        }
        Set<String> pedidas = Arrays.stream(valor.split(","))
                .map(item -> item.trim().toUpperCase(Locale.ROOT))
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (pedidas.isEmpty() || !COLUMNAS_POSICIONES.containsAll(pedidas)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay una columna de la tabla que no existe");
        }
        return COLUMNAS_POSICIONES.stream().filter(pedidas::contains).collect(Collectors.joining(","));
    }

    private FormatoPublicacion defecto(League league, String usuario) {
        FormatoPublicacion formato = new FormatoPublicacion();
        formato.setId(UUID.randomUUID());
        formato.setIdLiga(league.getId());
        formato.setNombreLugar("");
        String descripcion = league.getName() == null ? "" : league.getName().trim();
        formato.setDescripcion(descripcion.length() > 120 ? descripcion.substring(0, 120) : descripcion);
        formato.setLogoX(22);
        formato.setLogoY(12);
        formato.setLogoAncho(160);
        formato.setLogoAlto(180);
        formato.setLogoHabilitado(true);
        formato.setTituloHabilitado(true);
        formato.setSubtituloHabilitado(true);
        formato.setLugarHabilitado(true);
        formato.setFechaHabilitado(true);
        formato.setTituloNegrita(true);
        formato.setTituloCursiva(true);
        formato.setTituloFamilia("Arial Black");
        formato.setTituloColorInicio("#FFFFFF");
        formato.setTituloColorFin("#C5D0DC");
        formato.setTituloContornoColor("#061428");
        formato.setTituloContornoGrosor(3);
        formato.setTituloSombra(true);
        formato.setSubtituloNegrita(true);
        formato.setSubtituloCursiva(true);
        formato.setSubtituloFamilia("Arial");
        formato.setSubtituloColor("#1F5EFF");
        formato.setLugarCursiva(true);
        formato.setLugarFamilia("Arial");
        formato.setLugarColor("#FFFFFF");
        formato.setLugarRayaAncho(90);
        formato.setLugarRayaGrosor(2);
        formato.setFechaAncho(424);
        formato.setFechaAlto(48);
        formato.setFechaRadio(24);
        formato.setFechaBordeGrosor(2);
        formato.setFechaBordeColor("#FFFFFF");
        formato.setFechaNegrita(true);
        formato.setFechaFamilia("Arial");
        formato.setFechaColor("#FFFFFF");
        formato.setFechaIconoTamano(26);
        formato.setEstado("ACTIVO");
        OffsetDateTime ahora = OffsetDateTime.now();
        formato.setFechaCreacion(ahora);
        formato.setFechaModificacion(ahora);
        formato.setUsuarioCreacion(usuario);
        formato.setUsuarioModificacion(usuario);
        return formato;
    }

    private FormatoPublicacionTabla tablaDefecto(FormatoPublicacion formato, String tipo, String usuario) {
        FormatoPublicacionTabla tabla = new FormatoPublicacionTabla();
        tabla.setId(UUID.randomUUID());
        tabla.setIdFormatoPublicacion(formato.getId());
        tabla.setIdLiga(formato.getIdLiga());
        tabla.setTipo(tipo);
        tabla.setTablaX(28);
        tabla.setTablaY(276);
        tabla.setTablaAncho(968);
        tabla.setBordeGrosor(2);
        tabla.setBordeColor("#FFFFFF");
        tabla.setCabeceraAlto(44);
        tabla.setCabeceraFondo("#1A4FB8");
        tabla.setCabeceraColor("#FFFFFF");
        tabla.setCabeceraTamano(18);
        tabla.setFilaAlto(64);
        tabla.setFilaFondo("#071833");
        tabla.setFilaColor("#FFFFFF");
        tabla.setFilaTamano(22);
        tabla.setNumeroColor("#2F6BFF");
        tabla.setNumeroTamano(32);
        tabla.setDestacadaFondo("#1A56C9");
        tabla.setColumnasTabla("POSICIONES".equals(tipo) ? COLUMNAS_INICIALES : "");
        tabla.setTitulo(tituloDefecto(tipo));
        tabla.setTituloX(196);
        tabla.setTituloY(18);
        tabla.setTituloTamano(64);
        tabla.setSubtituloX(196);
        tabla.setSubtituloY(96);
        tabla.setSubtituloTamano(36);
        tabla.setFechaX(300);
        tabla.setFechaY(206);
        tabla.setFechaTamano(20);
        tabla.setLugarY(158);
        tabla.setLugarTamano(28);
        tabla.setFondoPublicacion(null);
        tabla.setAgruparPrimeraColumna(false);
        tabla.setEstado("ACTIVO");
        OffsetDateTime ahora = OffsetDateTime.now();
        tabla.setFechaCreacion(ahora);
        tabla.setFechaModificacion(ahora);
        tabla.setUsuarioCreacion(usuario);
        tabla.setUsuarioModificacion(usuario);
        return tabla;
    }

    private void auditar(FormatoPublicacion formato, String usuario) {
        formato.setFechaModificacion(OffsetDateTime.now());
        formato.setUsuarioModificacion(usuario);
    }

    private String tituloDefecto(String tipo) {
        if ("POSICIONES".equals(tipo)) {
            return "TABLA DE POSICIONES";
        }
        if ("RESULTADOS".equals(tipo)) {
            return "RESULTADOS";
        }
        if ("HORARIOS".equals(tipo)) {
            return "HORARIOS";
        }
        return "GOLEADORES";
    }

    private String fondoPublicacion(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        String valor = nombre.trim();
        if (valor.contains("..") || valor.indexOf('\\') >= 0
                || !valor.matches("(?i)(?:[A-Za-z0-9][A-Za-z0-9._-]{0,30}/)?[A-Za-z0-9][A-Za-z0-9._-]{0,40}\\.(jpg|jpeg|png|webp)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El fondo de la publicación debe ser un archivo jpg, png o webp, dentro de plantillas o de una carpeta");
        }
        return valor;
    }

    private String texto(String valor, String campo, int maximo, boolean vacio) {
        String limpio = valor == null ? "" : valor.trim();
        if (!vacio && limpio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " no puede quedar en blanco");
        }
        if (limpio.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, campo + " admite hasta " + maximo + " caracteres");
        }
        return limpio;
    }

    private int medida(Integer valor, String campo, int minimo, int maximo) {
        if (valor == null || valor < minimo || valor > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La medida de " + campo + " no es válida");
        }
        return valor;
    }

    private String color(String valor, String campo) {
        if (valor == null || !valor.trim().matches("(?i)#[0-9A-F]{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El color de " + campo + " debe ser #RRGGBB");
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }

    private String familia(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty() || valor.trim().length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La letra de " + campo + " es obligatoria");
        }
        return valor.trim();
    }

    private League liga(UUID leagueId) {
        return leagues.findById(leagueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La liga no existe"));
    }

    private String usuario() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private AppUser actual() {
        return users.findWithMembershipsByUsername(usuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void assertRead(UUID leagueId) {
        AppUser user = actual();
        boolean miembro = user.isMaster() || user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId));
        if (!miembro) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta liga");
        }
    }

    private void assertWrite(UUID leagueId) {
        AppUser user = actual();
        if (user.isMaster()) {
            return;
        }
        boolean admin = user.getMemberships().stream()
                .anyMatch(membership -> membership.getLeague().getId().equals(leagueId)
                        && "ADMINISTRATIVO".equals(membership.getRole()));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar esta liga");
        }
    }
}
