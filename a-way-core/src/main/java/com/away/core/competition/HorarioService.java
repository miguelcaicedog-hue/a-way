package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class HorarioService {

    private static final int DESCANSO = 80;
    private static final String[] ORDINALES = {
            "primero", "segundo", "tercero", "cuarto", "quinto",
            "sexto", "séptimo", "octavo", "noveno", "décimo",
            "undécimo", "duodécimo", "decimotercero", "decimocuarto", "decimoquinto",
            "decimosexto"
    };

    private final EditionRepository editions;
    private final CategoryRepository categories;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final CanchaRepository canchas;
    private final TeamRepository teams;
    private final GrupoRepository grupos;
    private final CruceRepository cruces;
    private final AppUserRepository users;

    public HorarioService(EditionRepository editions, CategoryRepository categories, JornadaRepository jornadas,
                          PartidoRepository partidos, CanchaRepository canchas, TeamRepository teams,
                          GrupoRepository grupos, CruceRepository cruces, AppUserRepository users) {
        this.editions = editions;
        this.categories = categories;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.canchas = canchas;
        this.teams = teams;
        this.grupos = grupos;
        this.cruces = cruces;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public HorarioViews.HorarioPanelView panel(UUID editionId, UUID categoryId, LocalDate fecha, List<Integer> jornadas, String aviso) {
        Edition edition = edition(editionId);
        assertRead(edition.getLeague().getId());
        return vista(edition, categoryId, fecha, jornadas, aviso);
    }

    @Transactional
    public HorarioViews.HorarioPanelView guardar(UUID id, HorarioViews.HorarioGuardarRequest request) {
        if (request.getEditionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona la edición");
        }
        Edition edition = edition(request.getEditionId());
        assertWrite(edition.getLeague().getId());
        Partido partido = partido(id);
        if (!"ACTIVO".equals(partido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El partido ya no está activo");
        }
        Contexto contexto = contexto(edition);
        if (!contexto.categoriasEdicion.contains(partido.getIdCategoria())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El partido no pertenece a esta edición");
        }
        LocalTime hora = parseHora(request.getHora());
        UUID canchaId = request.getCanchaId();
        if (hora == null && canchaId == null) {
            partido.setHora(null);
            partido.setIdCancha(null);
        } else if (hora == null || canchaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica la cancha y la hora, o deja ambas vacías");
        } else {
            LocalDate dia = fechaEfectiva(partido, contexto);
            if (dia == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El partido no tiene fecha");
            }
            Cancha cancha = canchaActiva(canchaId, edition.getLeague().getId());
            validarHueco(partido, cancha.getId(), minutos(hora), dia, contexto);
            partido.setHora(hora);
            partido.setIdCancha(cancha.getId());
        }
        auditar(partido, usuario());
        partidos.save(partido);
        return vista(edition, request.getCategoryId(), request.getFecha(), request.getJornadas(), null);
    }

    @Transactional
    public HorarioViews.HorarioPanelView asignarDia(HorarioViews.HorarioGuardarRequest request) {
        if (request.getEditionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona la edición");
        }
        if (request.getFecha() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige la fecha del día que quieres asignar");
        }
        Edition edition = edition(request.getEditionId());
        assertWrite(edition.getLeague().getId());
        Contexto contexto = contexto(edition);
        List<Cancha> activas = canchas.buscar(edition.getLeague().getId(), "", "ACTIVO");
        if (activas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay canchas activas en la liga");
        }
        List<Partido> pendientes = new ArrayList<>();
        for (Partido partido : contexto.partidos) {
            if (!contexto.categoriasEdicion.contains(partido.getIdCategoria())) {
                continue;
            }
            if (request.getCategoryId() != null && !request.getCategoryId().equals(partido.getIdCategoria())) {
                continue;
            }
            Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
            if (jornada == null || !"ACTIVO".equals(jornada.getEstado())) {
                continue;
            }
            if (!incluye(request.getJornadas(), jornada.getNumero())) {
                continue;
            }
            if (!request.getFecha().equals(fechaEfectiva(partido, contexto))) {
                continue;
            }
            if (partido.getHora() != null) {
                continue;
            }
            if (partido.getIdEquipoLocal() == null || partido.getIdEquipoVisita() == null) {
                continue;
            }
            pendientes.add(partido);
        }
        pendientes.sort(Comparator.comparing((Partido partido) -> nombreCategoria(partido, contexto))
                .thenComparing(partido -> contexto.porJornada.get(partido.getIdJornada()).getNumero())
                .thenComparing(Partido::getOrden));
        if (pendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay partidos con los dos equipos sin horario en esa fecha");
        }
        Ritmo ritmo = ritmo(request);
        ritmo.maxTurnos = Math.min(ritmo.maxTurnos, pendientes.size() * 2 + 4);
        Ocupacion ocupacion = ocupacionDelDia(request.getFecha(), contexto);
        Map<UUID, Asignacion> plan = new LinkedHashMap<>();
        boolean completo = buscar(pendientes, 0, ocupacion, plan, activas, ritmo, new int[]{0});
        String aviso = null;
        if (!completo) {
            plan.clear();
            ocupacion = ocupacionDelDia(request.getFecha(), contexto);
            List<String> sinLugar = new ArrayList<>();
            for (Partido partido : pendientes) {
                Asignacion asignacion = primerHueco(partido, ocupacion, activas, ritmo);
                if (asignacion == null) {
                    sinLugar.add(cruce(partido, contexto));
                    continue;
                }
                ocupacion.agregar(asignacion.canchaId, partido.getIdEquipoLocal(), partido.getIdEquipoVisita(), asignacion.minutos);
                plan.put(partido.getId(), asignacion);
            }
            if (!sinLugar.isEmpty()) {
                aviso = "Quedaron sin lugar: " + String.join(", ", sinLugar);
            }
        }
        String usuario = usuario();
        for (Partido partido : pendientes) {
            Asignacion asignacion = plan.get(partido.getId());
            if (asignacion == null) {
                continue;
            }
            partido.setIdCancha(asignacion.canchaId);
            partido.setHora(LocalTime.of(asignacion.minutos / 60, asignacion.minutos % 60));
            auditar(partido, usuario);
            partidos.save(partido);
        }
        return vista(edition, request.getCategoryId(), request.getFecha(), request.getJornadas(), aviso);
    }

    @Transactional
    public HorarioViews.HorarioPanelView quitarCancha(HorarioViews.HorarioGuardarRequest request) {
        if (request.getEditionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona la edición");
        }
        if (request.getCanchaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige la cancha");
        }
        if (request.getDia() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige el día de la cancha");
        }
        Edition edition = edition(request.getEditionId());
        assertWrite(edition.getLeague().getId());
        Cancha cancha = canchaDeLiga(request.getCanchaId(), edition.getLeague().getId());
        Contexto contexto = contexto(edition);
        String usuario = usuario();
        for (Partido partido : contexto.partidos) {
            if (!cancha.getId().equals(partido.getIdCancha())) {
                continue;
            }
            if (!contexto.categoriasEdicion.contains(partido.getIdCategoria())) {
                continue;
            }
            if (request.getCategoryId() != null && !request.getCategoryId().equals(partido.getIdCategoria())) {
                continue;
            }
            Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
            if (jornada == null || !"ACTIVO".equals(jornada.getEstado())) {
                continue;
            }
            if (!incluye(request.getJornadas(), jornada.getNumero())) {
                continue;
            }
            if (!request.getDia().equals(fechaEfectiva(partido, contexto))) {
                continue;
            }
            partido.setHora(null);
            partido.setIdCancha(null);
            auditar(partido, usuario);
            partidos.save(partido);
        }
        return vista(edition, request.getCategoryId(), request.getFecha(), request.getJornadas(), null);
    }

    private HorarioViews.HorarioPanelView vista(Edition edition, UUID categoryId, LocalDate fecha, List<Integer> jornadas, String aviso) {
        Contexto contexto = contexto(edition);
        if (categoryId != null && !contexto.categoriasEdicion.contains(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no pertenece a esta edición");
        }
        List<HorarioViews.HorarioCanchaView> canchasVista = new ArrayList<>();
        Map<UUID, String> nombreCancha = new HashMap<>();
        for (Cancha cancha : canchas.buscar(edition.getLeague().getId(), "", "")) {
            nombreCancha.put(cancha.getId(), cancha.getDescripcion());
            if ("ACTIVO".equals(cancha.getEstado())) {
                canchasVista.add(new HorarioViews.HorarioCanchaView(cancha.getId(), cancha.getDescripcion()));
            }
        }
        TreeSet<Integer> numeros = new TreeSet<>();
        List<HorarioViews.HorarioPartidoView> filas = new ArrayList<>();
        for (Partido partido : contexto.partidos) {
            if (!contexto.categoriasEdicion.contains(partido.getIdCategoria())) {
                continue;
            }
            Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
            if (jornada == null || !"ACTIVO".equals(jornada.getEstado())) {
                continue;
            }
            if (categoryId == null || categoryId.equals(partido.getIdCategoria())) {
                numeros.add(jornada.getNumero());
            }
            if (categoryId != null && !categoryId.equals(partido.getIdCategoria())) {
                continue;
            }
            if (!incluye(jornadas, jornada.getNumero())) {
                continue;
            }
            LocalDate dia = fechaEfectiva(partido, contexto);
            if (fecha != null && !fecha.equals(dia)) {
                continue;
            }
            boolean vuelta = jornada.getNombre().endsWith(" VUELTA");
            Category categoria = contexto.porCategoria.get(partido.getIdCategoria());
            filas.add(new HorarioViews.HorarioPartidoView(
                    partido.getId(),
                    partido.getIdCategoria(),
                    categoria == null ? "" : categoria.getName(),
                    jornada.getNumero(),
                    jornada.getNombre(),
                    partido.getOrden(),
                    dia,
                    textoHora(partido.getHora()),
                    partido.getIdCancha(),
                    partido.getIdCancha() == null ? null : nombreCancha.getOrDefault(partido.getIdCancha(), "Cancha"),
                    textoPartido(partido, true, vuelta, contexto),
                    textoPartido(partido, false, vuelta, contexto),
                    partido.getIdEquipoLocal() != null && partido.getIdEquipoVisita() != null));
        }
        filas.sort(Comparator.comparing((HorarioViews.HorarioPartidoView fila) -> fila.getFecha() == null ? LocalDate.MAX : fila.getFecha())
                .thenComparing(HorarioViews.HorarioPartidoView::getCategoryName)
                .thenComparing(HorarioViews.HorarioPartidoView::getJornadaNumero)
                .thenComparing(HorarioViews.HorarioPartidoView::getOrden));
        return new HorarioViews.HorarioPanelView(canchasVista, new ArrayList<>(numeros), filas, aviso);
    }

    private void validarHueco(Partido partido, UUID canchaId, int minutos, LocalDate dia, Contexto contexto) {
        for (Partido otro : contexto.partidos) {
            if (otro.getId().equals(partido.getId()) || otro.getHora() == null || otro.getIdCancha() == null) {
                continue;
            }
            if (!dia.equals(fechaEfectiva(otro, contexto))) {
                continue;
            }
            int otrosMinutos = minutos(otro.getHora());
            if (canchaId.equals(otro.getIdCancha()) && minutos == otrosMinutos) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa cancha ya tiene un partido a esa hora");
            }
            if (comparteEquipo(partido, otro) && Math.abs(minutos - otrosMinutos) < DESCANSO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un equipo ya juega ese día con menos de 80 minutos de diferencia");
            }
        }
    }

    private boolean comparteEquipo(Partido partido, Partido otro) {
        return mismo(partido.getIdEquipoLocal(), otro.getIdEquipoLocal())
                || mismo(partido.getIdEquipoLocal(), otro.getIdEquipoVisita())
                || mismo(partido.getIdEquipoVisita(), otro.getIdEquipoLocal())
                || mismo(partido.getIdEquipoVisita(), otro.getIdEquipoVisita());
    }

    private boolean mismo(UUID a, UUID b) {
        return a != null && a.equals(b);
    }

    private boolean incluye(List<Integer> jornadas, int numero) {
        return jornadas == null || jornadas.isEmpty() || jornadas.contains(numero);
    }

    private Ocupacion ocupacionDelDia(LocalDate dia, Contexto contexto) {
        Ocupacion ocupacion = new Ocupacion();
        for (Partido partido : contexto.partidos) {
            if (partido.getHora() == null || partido.getIdCancha() == null) {
                continue;
            }
            if (!dia.equals(fechaEfectiva(partido, contexto))) {
                continue;
            }
            ocupacion.agregar(partido.getIdCancha(), partido.getIdEquipoLocal(), partido.getIdEquipoVisita(), minutos(partido.getHora()));
        }
        return ocupacion;
    }

    private Ritmo ritmo(HorarioViews.HorarioGuardarRequest request) {
        LocalTime inicio = parseHora(request.getHoraInicio());
        if (inicio == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica la hora de inicio");
        }
        Integer intervalo = request.getIntervaloMinutos();
        if (intervalo == null || intervalo < 5 || intervalo > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El intervalo debe estar entre 5 y 180 minutos");
        }
        int comienzo = minutos(inicio);
        int cupo = 0;
        while (comienzo + (long) cupo * intervalo < 24 * 60) {
            cupo++;
        }
        if (cupo == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de inicio no deja turnos en el día");
        }
        Ritmo ritmo = new Ritmo();
        ritmo.inicio = comienzo;
        ritmo.paso = intervalo;
        ritmo.descanso = intervalo * 2;
        ritmo.maxTurnos = cupo;
        return ritmo;
    }

    private boolean buscar(List<Partido> pendientes, int indice, Ocupacion ocupacion, Map<UUID, Asignacion> plan,
                           List<Cancha> activas, Ritmo ritmo, int[] visitas) {
        if (indice == pendientes.size()) {
            return true;
        }
        if (visitas[0]++ > 200000) {
            return false;
        }
        Partido partido = pendientes.get(indice);
        for (Asignacion asignacion : huecos(partido, ocupacion, activas, ritmo)) {
            ocupacion.agregar(asignacion.canchaId, partido.getIdEquipoLocal(), partido.getIdEquipoVisita(), asignacion.minutos);
            plan.put(partido.getId(), asignacion);
            if (buscar(pendientes, indice + 1, ocupacion, plan, activas, ritmo, visitas)) {
                return true;
            }
            plan.remove(partido.getId());
            ocupacion.quitar(asignacion.canchaId, partido.getIdEquipoLocal(), partido.getIdEquipoVisita(), asignacion.minutos);
        }
        return false;
    }

    private Asignacion primerHueco(Partido partido, Ocupacion ocupacion, List<Cancha> activas, Ritmo ritmo) {
        List<Asignacion> opciones = huecos(partido, ocupacion, activas, ritmo);
        return opciones.isEmpty() ? null : opciones.get(0);
    }

    private List<Asignacion> huecos(Partido partido, Ocupacion ocupacion, List<Cancha> activas, Ritmo ritmo) {
        List<Asignacion> opciones = new ArrayList<>();
        for (int turno = 0; turno < ritmo.maxTurnos; turno++) {
            int minutos = ritmo.inicio + turno * ritmo.paso;
            for (int indice = 0; indice < activas.size(); indice++) {
                Cancha cancha = activas.get(indice);
                if (!ocupacion.libre(cancha.getId(), partido.getIdEquipoLocal(), partido.getIdEquipoVisita(), minutos, ritmo.descanso)) {
                    continue;
                }
                Asignacion asignacion = new Asignacion();
                asignacion.canchaId = cancha.getId();
                asignacion.minutos = minutos;
                asignacion.carga = ocupacion.carga(cancha.getId());
                asignacion.orden = indice;
                opciones.add(asignacion);
            }
        }
        opciones.sort(Comparator.comparingInt((Asignacion asignacion) -> asignacion.carga)
                .thenComparingInt(asignacion -> asignacion.minutos)
                .thenComparingInt(asignacion -> asignacion.orden));
        return opciones;
    }

    private Cancha canchaActiva(UUID id, UUID leagueId) {
        Cancha cancha = canchaDeLiga(id, leagueId);
        if (!"ACTIVO".equals(cancha.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cancha debe estar activa y ser de la liga");
        }
        return cancha;
    }

    private Cancha canchaDeLiga(UUID id, UUID leagueId) {
        Cancha cancha = canchas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cancha no existe"));
        if (!leagueId.equals(cancha.getIdLiga())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cancha no es de la liga");
        }
        return cancha;
    }

    private LocalDate fechaEfectiva(Partido partido, Contexto contexto) {
        if (partido.getFecha() != null) {
            return partido.getFecha();
        }
        Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
        return jornada == null ? null : jornada.getFecha();
    }

    private String nombreCategoria(Partido partido, Contexto contexto) {
        Category categoria = contexto.porCategoria.get(partido.getIdCategoria());
        return categoria == null ? "" : categoria.getName();
    }

    private String cruce(Partido partido, Contexto contexto) {
        Jornada jornada = contexto.porJornada.get(partido.getIdJornada());
        boolean vuelta = jornada != null && jornada.getNombre().endsWith(" VUELTA");
        return textoPartido(partido, true, vuelta, contexto) + " contra " + textoPartido(partido, false, vuelta, contexto);
    }

    private Contexto contexto(Edition edition) {
        Contexto contexto = new Contexto();
        List<UUID> categorias = new ArrayList<>();
        for (Category categoria : categories.findByEdition_IdOrderByNameAsc(edition.getId())) {
            contexto.categoriasEdicion.add(categoria.getId());
        }
        for (Edition item : editions.findByLeague_IdOrderByStartsOnDesc(edition.getLeague().getId())) {
            for (Category categoria : categories.findByEdition_IdOrderByNameAsc(item.getId())) {
                contexto.porCategoria.put(categoria.getId(), categoria);
                categorias.add(categoria.getId());
            }
        }
        if (categorias.isEmpty()) {
            return contexto;
        }
        for (Jornada jornada : jornadas.findByIdCategoriaInAndEstado(categorias, "ACTIVO")) {
            contexto.porJornada.put(jornada.getId(), jornada);
        }
        contexto.partidos.addAll(partidos.findByIdCategoriaInAndEstado(categorias, "ACTIVO"));
        for (UUID categoriaId : categorias) {
            for (Grupo grupo : grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoriaId, "ACTIVO")) {
                contexto.porGrupo.put(grupo.getId(), grupo);
            }
            for (Cruce cruce : cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoriaId, "ACTIVO")) {
                contexto.porCruce.put(cruce.getId(), cruce);
            }
        }
        return contexto;
    }

    private String textoHora(LocalTime hora) {
        if (hora == null) {
            return null;
        }
        return String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }

    private LocalTime parseHora(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String valor = texto.trim();
        if (!valor.matches("\\d{1,2}:\\d{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora debe tener el formato hh:mm");
        }
        String[] partes = valor.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = Integer.parseInt(partes[1]);
        if (horas > 23 || minutos > 59) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora debe tener el formato hh:mm");
        }
        return LocalTime.of(horas, minutos);
    }

    private int minutos(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }

    private String texto(UUID equipoId, Integer posicion, UUID grupoId, Contexto contexto) {
        if (equipoId != null) {
            Team equipo = contexto.porEquipo.get(equipoId);
            if (equipo == null && !contexto.porEquipo.containsKey(equipoId)) {
                equipo = teams.findById(equipoId).orElse(null);
                if (equipo != null) {
                    contexto.porEquipo.put(equipoId, equipo);
                }
            }
            return equipo == null ? "EQUIPO" : equipo.getName();
        }
        if (posicion != null) {
            Grupo grupo = grupoId == null ? null : contexto.porGrupo.get(grupoId);
            String ordinal = posicion >= 1 && posicion <= ORDINALES.length ? ORDINALES[posicion - 1] : posicion + "º";
            String nombre = grupo == null ? "GRUPO" : grupo.getNombre();
            return (ordinal + " " + nombre).toUpperCase(Locale.forLanguageTag("es"));
        }
        return "LIBRE";
    }

    private String textoPartido(Partido partido, boolean local, boolean vuelta, Contexto contexto) {
        UUID equipoId = local ? partido.getIdEquipoLocal() : partido.getIdEquipoVisita();
        Integer posicion = local ? partido.getPosicionLocal() : partido.getPosicionVisita();
        UUID grupoId = local ? partido.getIdGrupoLocal() : partido.getIdGrupoVisita();
        if (equipoId != null || posicion != null) {
            return texto(equipoId, posicion, grupoId, contexto);
        }
        Cruce cruce = partido.getIdCruce() == null ? null : contexto.porCruce.get(partido.getIdCruce());
        if (cruce == null) {
            return "LIBRE";
        }
        String etiqueta = lado(cruce, vuelta ? !local : local, contexto);
        return etiqueta == null ? "LIBRE" : etiqueta;
    }

    private String lado(Cruce cruce, boolean local, Contexto contexto) {
        Integer posicion = local ? cruce.getPosicionLocal() : cruce.getPosicionVisita();
        UUID grupoId = local ? cruce.getIdGrupoLocal() : cruce.getIdGrupoVisita();
        if (posicion != null && grupoId != null) {
            return texto(null, posicion, grupoId, contexto);
        }
        UUID origenId = local ? cruce.getIdCruceOrigenLocal() : cruce.getIdCruceOrigenVisita();
        if (origenId == null) {
            return null;
        }
        Cruce origen = contexto.porCruce.get(origenId);
        return origen == null ? null : avanza(origen, contexto);
    }

    private String avanza(Cruce cruce, Contexto contexto) {
        String local = lado(cruce, true, contexto);
        String visita = lado(cruce, false, contexto);
        if (local == null && visita == null) {
            return null;
        }
        if (local == null) {
            return visita;
        }
        if (visita == null) {
            return local;
        }
        return "Ganador " + cruce.getOrden();
    }

    private Edition edition(UUID id) {
        return editions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La edición no existe"));
    }

    private Partido partido(UUID id) {
        return partidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El partido no existe"));
    }

    private void auditar(Partido partido, String usuario) {
        partido.setFechaModificacion(OffsetDateTime.now());
        partido.setUsuarioModificacion(usuario);
    }

    private String usuario() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
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

    private static final class Contexto {
        private final Map<UUID, Category> porCategoria = new HashMap<>();
        private final Set<UUID> categoriasEdicion = new HashSet<>();
        private final Map<UUID, Jornada> porJornada = new HashMap<>();
        private final List<Partido> partidos = new ArrayList<>();
        private final Map<UUID, Grupo> porGrupo = new HashMap<>();
        private final Map<UUID, Cruce> porCruce = new HashMap<>();
        private final Map<UUID, Team> porEquipo = new HashMap<>();
    }

    private static final class Ritmo {
        private int inicio;
        private int paso;
        private int descanso;
        private int maxTurnos;
    }

    private static final class Asignacion {
        private UUID canchaId;
        private int minutos;
        private int carga;
        private int orden;
    }

    private static final class Ocupacion {
        private final Map<UUID, Set<Integer>> porCancha = new HashMap<>();
        private final Map<UUID, List<Integer>> porEquipo = new HashMap<>();
        private final Map<UUID, Integer> carga = new HashMap<>();

        private void agregar(UUID canchaId, UUID local, UUID visita, int minutos) {
            porCancha.computeIfAbsent(canchaId, key -> new HashSet<>()).add(minutos);
            carga.merge(canchaId, 1, Integer::sum);
            agregarEquipo(local, minutos);
            agregarEquipo(visita, minutos);
        }

        private void quitar(UUID canchaId, UUID local, UUID visita, int minutos) {
            Set<Integer> horas = porCancha.get(canchaId);
            if (horas != null) {
                horas.remove(minutos);
            }
            carga.merge(canchaId, -1, Integer::sum);
            quitarEquipo(local, minutos);
            quitarEquipo(visita, minutos);
        }

        private int carga(UUID canchaId) {
            return carga.getOrDefault(canchaId, 0);
        }

        private boolean libre(UUID canchaId, UUID local, UUID visita, int minutos, int descanso) {
            if (porCancha.getOrDefault(canchaId, new HashSet<>()).contains(minutos)) {
                return false;
            }
            return descansa(local, minutos, descanso) && descansa(visita, minutos, descanso);
        }

        private boolean descansa(UUID equipo, int minutos, int descanso) {
            if (equipo == null) {
                return true;
            }
            for (int existente : porEquipo.getOrDefault(equipo, new ArrayList<>())) {
                if (Math.abs(existente - minutos) < descanso) {
                    return false;
                }
            }
            return true;
        }

        private void agregarEquipo(UUID equipo, int minutos) {
            if (equipo != null) {
                porEquipo.computeIfAbsent(equipo, key -> new ArrayList<>()).add(minutos);
            }
        }

        private void quitarEquipo(UUID equipo, int minutos) {
            if (equipo == null) {
                return;
            }
            List<Integer> lista = porEquipo.get(equipo);
            if (lista != null) {
                lista.remove(Integer.valueOf(minutos));
            }
        }
    }
}
