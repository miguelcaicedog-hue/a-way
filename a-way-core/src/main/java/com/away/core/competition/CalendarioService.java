package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class CalendarioService {

    private static final String[] ORDINALES = {
            "primero", "segundo", "tercero", "cuarto", "quinto",
            "sexto", "séptimo", "octavo", "noveno", "décimo",
            "undécimo", "duodécimo", "decimotercero", "decimocuarto", "decimoquinto",
            "decimosexto", "decimoséptimo", "decimoctavo", "decimonoveno", "vigésimo"
    };

    private final CategoryRepository categories;
    private final GrupoRepository grupos;
    private final GrupoEquipoRepository asignaciones;
    private final FaseRepository fases;
    private final CruceRepository cruces;
    private final TeamRepository teams;
    private final JornadaRepository jornadas;
    private final PartidoRepository partidos;
    private final AppUserRepository users;

    public CalendarioService(CategoryRepository categories, GrupoRepository grupos, GrupoEquipoRepository asignaciones,
                             FaseRepository fases, CruceRepository cruces, TeamRepository teams,
                             JornadaRepository jornadas, PartidoRepository partidos, AppUserRepository users) {
        this.categories = categories;
        this.grupos = grupos;
        this.asignaciones = asignaciones;
        this.fases = fases;
        this.cruces = cruces;
        this.teams = teams;
        this.jornadas = jornadas;
        this.partidos = partidos;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CalendarioPanelView panel(UUID categoryId, UUID groupId, boolean todos) {
        Category category = category(categoryId);
        assertRead(category.getEdition().getLeague().getId());
        return vista(category, groupId, todos);
    }

    @Transactional
    public CalendarioPanelView generar(UUID categoryId, UUID groupId, boolean todos) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        List<Jornada> activas = jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(category.getId(), "ACTIVO");
        List<Fase> rondas = fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        if (rondas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crea las fases de la categoría antes de generar el calendario.");
        }
        Fase gruposFase = rondas.get(0);
        Map<UUID, Cruce> porCruce = new HashMap<>();
        for (Cruce cruce : cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            porCruce.put(cruce.getId(), cruce);
        }
        List<Grupo> listaGrupos = grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        Map<UUID, Grupo> porGrupo = new HashMap<>();
        for (Grupo grupo : listaGrupos) {
            porGrupo.put(grupo.getId(), grupo);
        }
        TreeMap<Integer, List<PartidoBorrador>> porFecha = new TreeMap<>();
        int siguiente = 0;
        if (activas.isEmpty()) {
            boolean idaYVuelta = "IDA_Y_VUELTA".equals(gruposFase.getModalidad());
            Map<UUID, List<Team>> equipos = equiposPorGrupo(category.getId());
            for (Grupo grupo : listaGrupos) {
                for (Pareja pareja : sortear(equipos.getOrDefault(grupo.getId(), List.of()), idaYVuelta)) {
                    PartidoBorrador borrador = new PartidoBorrador();
                    borrador.faseId = gruposFase.getId();
                    borrador.grupoId = grupo.getId();
                    borrador.equipoLocal = pareja.local;
                    borrador.equipoVisita = pareja.visita;
                    borrador.nombre = "JORNADA " + pareja.fecha;
                    porFecha.computeIfAbsent(pareja.fecha, key -> new ArrayList<>()).add(borrador);
                    siguiente = Math.max(siguiente, pareja.fecha);
                }
            }
        } else {
            for (Jornada jornada : activas) {
                siguiente = Math.max(siguiente, jornada.getNumero());
            }
        }
        Set<UUID> fasesArmadas = new HashSet<>();
        for (Jornada jornada : activas) {
            fasesArmadas.add(jornada.getIdFase());
        }
        completarFasesSiguientes(category, gruposFase, fasesArmadas, siguiente, porFecha, porCruce, porGrupo);
        if (!activas.isEmpty() && porFecha.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El calendario de la categoría ya está creado.");
        }
        if (porFecha.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay equipos en los grupos ni cruces completos para armar partidos.");
        }
        persistir(category.getId(), porFecha, usuario());
        return vista(category, groupId, todos);
    }

    @Transactional
    public CalendarioPanelView cargar(UUID categoryId, UUID groupId, MultipartFile archivo) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        if (groupId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un grupo de la primera fase.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un archivo de Excel");
        }
        String nombre = archivo.getOriginalFilename() == null ? "calendario.xlsx" : archivo.getOriginalFilename();
        if (!nombre.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser .xlsx");
        }
        Grupo grupo = grupos.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El grupo no existe"));
        if (!category.getId().equals(grupo.getIdCategoria()) || !"ACTIVO".equals(grupo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grupo no pertenece a la categoría.");
        }
        List<Fase> rondas = fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        if (rondas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crea las fases de la categoría antes de cargar el calendario.");
        }
        Fase gruposFase = rondas.get(0);
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            if (groupId.equals(partido.getIdGrupo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grupo ya tiene calendario.");
            }
        }
        List<CalendarioWorkbook.PartidoLeido> leidos;
        try {
            leidos = CalendarioWorkbook.leer(archivo.getInputStream());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo de Excel");
        }
        if (leidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no tiene partidos");
        }
        Map<String, Team> porNombre = new HashMap<>();
        for (GrupoEquipo puesto : asignaciones.findByIdGrupoAndEstado(groupId, "ACTIVO")) {
            Team equipo = puesto.getEquipo();
            if (equipo == null || !"ACTIVO".equals(equipo.getEstado())) {
                continue;
            }
            porNombre.put(CalendarioWorkbook.clave(equipo.getName()), equipo);
        }
        LinkedHashSet<String> faltantes = new LinkedHashSet<>();
        List<PartidoBorrador> borradores = new ArrayList<>();
        for (CalendarioWorkbook.PartidoLeido leido : leidos) {
            Team local = porNombre.get(CalendarioWorkbook.clave(leido.local));
            Team visita = porNombre.get(CalendarioWorkbook.clave(leido.visita));
            if (local == null) {
                faltantes.add(leido.local);
            }
            if (visita == null) {
                faltantes.add(leido.visita);
            }
            if (local != null && visita != null && local.getId().equals(visita.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fila " + leido.fila + " tiene el mismo equipo como local y visitante.");
            }
            PartidoBorrador borrador = new PartidoBorrador();
            borrador.faseId = gruposFase.getId();
            borrador.grupoId = groupId;
            borrador.equipoLocal = local == null ? null : local.getId();
            borrador.equipoVisita = visita == null ? null : visita.getId();
            borrador.nombre = "JORNADA " + leido.jornada;
            borrador.numero = leido.jornada;
            borrador.fecha = leido.fecha;
            borradores.add(borrador);
        }
        if (!faltantes.isEmpty()) {
            throw new CalendarioCargaException(new ArrayList<>(faltantes));
        }
        Map<Integer, Jornada> porNumero = new HashMap<>();
        Map<UUID, Integer> siguienteOrden = new HashMap<>();
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(category.getId(), "ACTIVO")) {
            porNumero.put(jornada.getNumero(), jornada);
            siguienteOrden.put(jornada.getId(), 1);
        }
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            siguienteOrden.merge(partido.getIdJornada(), partido.getOrden() + 1, Math::max);
        }
        String usuario = usuario();
        for (PartidoBorrador borrador : borradores) {
            Jornada jornada = porNumero.get(borrador.numero);
            if (jornada == null) {
                jornada = guardarJornada(category.getId(), gruposFase.getId(), borrador.numero, borrador.nombre, usuario);
                jornada.setFecha(borrador.fecha);
                porNumero.put(borrador.numero, jornada);
                siguienteOrden.put(jornada.getId(), 1);
            } else if (!gruposFase.getId().equals(jornada.getIdFase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La jornada " + borrador.numero + " ya pertenece a otra fase.");
            } else if (jornada.getFecha() == null && borrador.fecha != null) {
                jornada.setFecha(borrador.fecha);
                auditar(jornada, usuario, false);
            }
            int orden = siguienteOrden.getOrDefault(jornada.getId(), 1);
            guardarPartido(category.getId(), jornada.getId(), borrador, orden, usuario);
            siguienteOrden.put(jornada.getId(), orden + 1);
        }
        recalcularFasesSiguientes(category, gruposFase, usuario);
        return vista(category, groupId);
    }

    @Transactional
    public CalendarioPanelView fechaJornada(UUID jornadaId, LocalDate fecha, UUID groupId, boolean todos) {
        Jornada jornada = jornadas.findById(jornadaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La jornada no existe"));
        Category category = category(jornada.getIdCategoria());
        assertWrite(category.getEdition().getLeague().getId());
        if (!"ACTIVO".equals(jornada.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La jornada ya no está activa");
        }
        String usuario = usuario();
        jornada.setFecha(fecha);
        auditar(jornada, usuario, false);
        for (Partido partido : partidos.findByIdJornadaAndEstadoOrderByOrdenAsc(jornada.getId(), "ACTIVO")) {
            partido.setFecha(fecha);
            auditar(partido, usuario, false);
        }
        return vista(category, groupId, todos);
    }

    @Transactional
    public CalendarioPanelView fechaPartido(UUID partidoId, LocalDate fecha, UUID groupId, boolean todos) {
        Partido partido = partidos.findById(partidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El partido no existe"));
        Category category = category(partido.getIdCategoria());
        assertWrite(category.getEdition().getLeague().getId());
        if (!"ACTIVO".equals(partido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El partido ya no está activo");
        }
        partido.setFecha(fecha);
        auditar(partido, usuario(), false);
        return vista(category, groupId, todos);
    }

    @Transactional
    public CalendarioPanelView eliminar(UUID categoryId, UUID groupId, boolean todos) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        List<Jornada> activas = jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(category.getId(), "ACTIVO");
        if (activas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no tiene calendario activo");
        }
        String usuario = usuario();
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            partido.setEstado("ELIMINADO");
            auditar(partido, usuario, false);
        }
        for (Jornada jornada : activas) {
            jornada.setEstado("ELIMINADO");
            auditar(jornada, usuario, false);
        }
        return vista(category, groupId, todos);
    }

    private void recalcularFasesSiguientes(Category category, Fase gruposFase, String usuario) {
        List<Fase> rondas = fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        Set<UUID> siguientes = new HashSet<>();
        for (Fase fase : rondas) {
            if (!fase.getId().equals(gruposFase.getId())) {
                siguientes.add(fase.getId());
            }
        }
        if (siguientes.isEmpty()) {
            return;
        }
        int maxPrimera = 0;
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(category.getId(), "ACTIVO")) {
            if (gruposFase.getId().equals(jornada.getIdFase())) {
                maxPrimera = Math.max(maxPrimera, jornada.getNumero());
            } else if (siguientes.contains(jornada.getIdFase())) {
                jornada.setEstado("ELIMINADO");
                auditar(jornada, usuario, false);
            }
        }
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            if (siguientes.contains(partido.getIdFase())) {
                partido.setEstado("ELIMINADO");
                auditar(partido, usuario, false);
            }
        }
        jornadas.flush();
        partidos.flush();
        Map<UUID, Cruce> porCruce = new HashMap<>();
        for (Cruce cruce : cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            porCruce.put(cruce.getId(), cruce);
        }
        Map<UUID, Grupo> porGrupo = new HashMap<>();
        for (Grupo grupo : grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            porGrupo.put(grupo.getId(), grupo);
        }
        TreeMap<Integer, List<PartidoBorrador>> porFecha = new TreeMap<>();
        completarFasesSiguientes(category, gruposFase, Set.of(), maxPrimera, porFecha, porCruce, porGrupo);
        persistir(category.getId(), porFecha, usuario);
    }

    private void completarFasesSiguientes(Category category, Fase gruposFase, Set<UUID> fasesArmadas, int siguiente,
                                          Map<Integer, List<PartidoBorrador>> porFecha, Map<UUID, Cruce> porCruce,
                                          Map<UUID, Grupo> porGrupo) {
        for (Fase fase : fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            if (fase.getId().equals(gruposFase.getId()) || fasesArmadas.contains(fase.getId())) {
                continue;
            }
            List<Cruce> serie = new ArrayList<>();
            for (Cruce cruce : cruces.findByIdFaseAndEstadoOrderByOrdenAsc(fase.getId(), "ACTIVO")) {
                if (lado(cruce, true, porCruce, porGrupo) != null && lado(cruce, false, porCruce, porGrupo) != null) {
                    serie.add(cruce);
                }
            }
            if (serie.isEmpty()) {
                continue;
            }
            boolean vuelta = "IDA_Y_VUELTA".equals(fase.getModalidad());
            siguiente++;
            String nombre = vuelta ? fase.getNombre() + " IDA" : fase.getNombre();
            agregarCruces(porFecha, siguiente, fase, serie, nombre, false);
            if (vuelta) {
                siguiente++;
                agregarCruces(porFecha, siguiente, fase, serie, fase.getNombre() + " VUELTA", true);
            }
        }
    }

    private void persistir(UUID categoryId, Map<Integer, List<PartidoBorrador>> porFecha, String usuario) {
        for (Map.Entry<Integer, List<PartidoBorrador>> entrada : porFecha.entrySet()) {
            PartidoBorrador primero = entrada.getValue().get(0);
            Jornada jornada = guardarJornada(categoryId, primero.faseId, entrada.getKey(), primero.nombre, usuario);
            int orden = 1;
            for (PartidoBorrador borrador : entrada.getValue()) {
                guardarPartido(categoryId, jornada.getId(), borrador, orden, usuario);
                orden++;
            }
        }
    }

    private void agregarCruces(Map<Integer, List<PartidoBorrador>> porFecha, int fecha, Fase fase, List<Cruce> serie,
                               String nombre, boolean invertir) {
        for (Cruce cruce : serie) {
            PartidoBorrador borrador = new PartidoBorrador();
            borrador.faseId = fase.getId();
            borrador.cruceId = cruce.getId();
            borrador.nombre = nombre;
            if (invertir) {
                borrador.posicionLocal = cruce.getPosicionVisita();
                borrador.grupoLocal = cruce.getIdGrupoVisita();
                borrador.posicionVisita = cruce.getPosicionLocal();
                borrador.grupoVisita = cruce.getIdGrupoLocal();
            } else {
                borrador.posicionLocal = cruce.getPosicionLocal();
                borrador.grupoLocal = cruce.getIdGrupoLocal();
                borrador.posicionVisita = cruce.getPosicionVisita();
                borrador.grupoVisita = cruce.getIdGrupoVisita();
            }
            porFecha.computeIfAbsent(fecha, key -> new ArrayList<>()).add(borrador);
        }
    }

    private List<Pareja> sortear(List<Team> equipos, boolean idaYVuelta) {
        if (equipos.size() < 2) {
            return List.of();
        }
        List<Team> orden = new ArrayList<>(equipos);
        Collections.shuffle(orden);
        int cantidad = orden.size();
        int qTeam = cantidad % 2 == 1 ? cantidad + 1 : cantidad;
        Team[] slot = new Team[qTeam + 1];
        for (int indice = 0; indice < cantidad; indice++) {
            slot[indice + 1] = orden.get(indice);
        }
        List<Pareja> pares = new ArrayList<>();
        for (int i = 1; i <= qTeam - 1; i++) {
            String lado = "L";
            int fecha = 2 * i - 1;
            int j = i + 1;
            for (; j <= qTeam - 1; j++) {
                fecha = fecha + 1;
                if (fecha >= qTeam) {
                    fecha = fecha % (qTeam - 1);
                }
                lado = "L".equals(lado) ? "V" : "L";
                pares.add(pareja(fecha, slot, i, j, lado));
            }
            fecha = 2 * i - 1;
            if (fecha >= qTeam) {
                fecha = fecha % (qTeam - 1);
            }
            lado = i > qTeam / 2 ? "V" : "L";
            pares.add(pareja(fecha, slot, i, j, lado));
        }
        if (idaYVuelta) {
            int desplazamiento = qTeam - 1;
            int hasta = pares.size();
            for (int indice = 0; indice < hasta; indice++) {
                Pareja ida = pares.get(indice);
                Pareja vuelta = new Pareja();
                vuelta.fecha = desplazamiento + ida.fecha;
                vuelta.local = ida.visita;
                vuelta.visita = ida.local;
                pares.add(vuelta);
            }
        }
        pares.removeIf(pareja -> pareja.local == null || pareja.visita == null);
        return pares;
    }

    private Pareja pareja(int fecha, Team[] slot, int i, int j, String lado) {
        Pareja pareja = new Pareja();
        pareja.fecha = fecha;
        if ("V".equals(lado)) {
            pareja.local = slot[j] == null ? null : slot[j].getId();
            pareja.visita = slot[i] == null ? null : slot[i].getId();
        } else {
            pareja.local = slot[i] == null ? null : slot[i].getId();
            pareja.visita = slot[j] == null ? null : slot[j].getId();
        }
        return pareja;
    }

    private Map<UUID, List<Team>> equiposPorGrupo(UUID categoryId) {
        Map<UUID, List<GrupoEquipo>> puestos = new HashMap<>();
        for (GrupoEquipo puesto : asignaciones.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            if (!"ACTIVO".equals(puesto.getEquipo().getEstado())) {
                continue;
            }
            puestos.computeIfAbsent(puesto.getIdGrupo(), key -> new ArrayList<>()).add(puesto);
        }
        Map<UUID, List<Team>> equipos = new HashMap<>();
        for (Map.Entry<UUID, List<GrupoEquipo>> entrada : puestos.entrySet()) {
            entrada.getValue().sort(Comparator.comparingInt(GrupoEquipo::getOrden));
            List<Team> lista = new ArrayList<>();
            for (GrupoEquipo puesto : entrada.getValue()) {
                lista.add(puesto.getEquipo());
            }
            equipos.put(entrada.getKey(), lista);
        }
        return equipos;
    }

    private CalendarioPanelView vista(Category category, UUID groupId) {
        return vista(category, groupId, false);
    }

    private CalendarioPanelView vista(Category category, UUID groupId, boolean todos) {
        Map<UUID, Team> porEquipo = new HashMap<>();
        Map<UUID, Grupo> porGrupo = new HashMap<>();
        List<CalendarioGrupoView> gruposVista = new ArrayList<>();
        for (Grupo grupo : grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            porGrupo.put(grupo.getId(), grupo);
            gruposVista.add(new CalendarioGrupoView(grupo.getId(), grupo.getNombre()));
        }
        UUID seleccionado = todos ? null : groupId;
        if (seleccionado != null && !porGrupo.containsKey(seleccionado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grupo no pertenece a la categoría.");
        }
        if (!todos && seleccionado == null && !gruposVista.isEmpty()) {
            seleccionado = gruposVista.get(0).getId();
        }
        Map<UUID, Cruce> porCruce = new HashMap<>();
        for (Cruce cruce : cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            porCruce.put(cruce.getId(), cruce);
        }
        Map<UUID, String> nombreJornada = new HashMap<>();
        List<Jornada> activas = jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(category.getId(), "ACTIVO");
        for (Jornada jornada : activas) {
            nombreJornada.put(jornada.getId(), jornada.getNombre());
        }
        Map<UUID, List<PartidoView>> porJornada = new HashMap<>();
        for (Partido partido : partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            if (seleccionado != null && !seleccionado.equals(partido.getIdGrupo())) {
                continue;
            }
            boolean vuelta = nombreJornada.getOrDefault(partido.getIdJornada(), "").endsWith(" VUELTA");
            porJornada.computeIfAbsent(partido.getIdJornada(), key -> new ArrayList<>())
                    .add(new PartidoView(partido.getId(), partido.getOrden(),
                            textoPartido(partido, true, vuelta, porEquipo, porGrupo, porCruce),
                            textoPartido(partido, false, vuelta, porEquipo, porGrupo, porCruce),
                            partido.getFecha()));
        }
        List<JornadaView> vistas = new ArrayList<>();
        for (Jornada jornada : activas) {
            List<PartidoView> deGrupo = porJornada.getOrDefault(jornada.getId(), List.of());
            if (deGrupo.isEmpty()) {
                continue;
            }
            vistas.add(new JornadaView(jornada.getId(), jornada.getNumero(), jornada.getNombre(), jornada.getFecha(),
                    deGrupo));
        }
        return new CalendarioPanelView(category.getId(), category.getName(), seleccionado, gruposVista, vistas);
    }

    private String texto(UUID equipoId, Integer posicion, UUID grupoId, Map<UUID, Team> equipos, Map<UUID, Grupo> grupos) {
        if (equipoId != null) {
            Team equipo = equipos.get(equipoId);
            if (equipo == null && !equipos.containsKey(equipoId)) {
                equipo = this.teams.findById(equipoId).orElse(null);
                if (equipo != null) {
                    equipos.put(equipoId, equipo);
                }
            }
            return equipo == null ? "EQUIPO" : equipo.getName();
        }
        if (posicion != null) {
            Grupo grupo = grupoId == null ? null : grupos.get(grupoId);
            if (grupo == null && grupoId != null) {
                grupo = this.grupos.findById(grupoId).orElse(null);
            }
            String ordinal = posicion >= 1 && posicion <= ORDINALES.length ? ORDINALES[posicion - 1] : posicion + "º";
            String nombre = grupo == null ? "GRUPO" : grupo.getNombre();
            return (ordinal + " " + nombre).toUpperCase(Locale.forLanguageTag("es"));
        }
        return "LIBRE";
    }

    private String textoPartido(Partido partido, boolean local, boolean vuelta, Map<UUID, Team> equipos,
                                Map<UUID, Grupo> grupos, Map<UUID, Cruce> cruces) {
        UUID equipoId = local ? partido.getIdEquipoLocal() : partido.getIdEquipoVisita();
        Integer posicion = local ? partido.getPosicionLocal() : partido.getPosicionVisita();
        UUID grupoId = local ? partido.getIdGrupoLocal() : partido.getIdGrupoVisita();
        if (equipoId != null || posicion != null) {
            return texto(equipoId, posicion, grupoId, equipos, grupos);
        }
        Cruce cruce = partido.getIdCruce() == null ? null : cruces.get(partido.getIdCruce());
        if (cruce == null) {
            return "LIBRE";
        }
        String etiqueta = lado(cruce, vuelta ? !local : local, cruces, grupos);
        return etiqueta == null ? "LIBRE" : etiqueta;
    }

    private String lado(Cruce cruce, boolean local, Map<UUID, Cruce> porId, Map<UUID, Grupo> porGrupo) {
        Integer posicion = local ? cruce.getPosicionLocal() : cruce.getPosicionVisita();
        UUID grupoId = local ? cruce.getIdGrupoLocal() : cruce.getIdGrupoVisita();
        if (posicion != null && grupoId != null) {
            return texto(null, posicion, grupoId, new HashMap<>(), porGrupo);
        }
        UUID origenId = local ? cruce.getIdCruceOrigenLocal() : cruce.getIdCruceOrigenVisita();
        if (origenId == null) {
            return null;
        }
        Cruce origen = porId.get(origenId);
        return origen == null ? null : avanza(origen, porId, porGrupo);
    }

    private String avanza(Cruce cruce, Map<UUID, Cruce> porId, Map<UUID, Grupo> porGrupo) {
        String local = lado(cruce, true, porId, porGrupo);
        String visita = lado(cruce, false, porId, porGrupo);
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

    private Jornada guardarJornada(UUID categoryId, UUID faseId, int numero, String nombre, String usuario) {
        Jornada jornada = new Jornada();
        jornada.setId(UUID.randomUUID());
        jornada.setIdCategoria(categoryId);
        jornada.setIdFase(faseId);
        jornada.setNumero(numero);
        jornada.setNombre(nombre);
        jornada.setEstadoCierre("ABIERTA");
        auditar(jornada, usuario, true);
        return jornadas.save(jornada);
    }

    private void guardarPartido(UUID categoryId, UUID jornadaId, PartidoBorrador borrador, int orden, String usuario) {
        Partido partido = new Partido();
        partido.setId(UUID.randomUUID());
        partido.setIdJornada(jornadaId);
        partido.setIdCategoria(categoryId);
        partido.setIdFase(borrador.faseId);
        partido.setIdGrupo(borrador.grupoId);
        partido.setIdCruce(borrador.cruceId);
        partido.setIdEquipoLocal(borrador.equipoLocal);
        partido.setIdEquipoVisita(borrador.equipoVisita);
        partido.setPosicionLocal(borrador.posicionLocal);
        partido.setIdGrupoLocal(borrador.grupoLocal);
        partido.setPosicionVisita(borrador.posicionVisita);
        partido.setIdGrupoVisita(borrador.grupoVisita);
        partido.setOrden(orden);
        partido.setFecha(borrador.fecha);
        auditar(partido, usuario, true);
        partidos.save(partido);
    }

    private Category category(UUID id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
    }

    private void auditar(Jornada jornada, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            jornada.setFechaCreacion(ahora);
            jornada.setUsuarioCreacion(usuario);
            jornada.setEstado("ACTIVO");
        }
        jornada.setFechaModificacion(ahora);
        jornada.setUsuarioModificacion(usuario);
    }

    private void auditar(Partido partido, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            partido.setFechaCreacion(ahora);
            partido.setUsuarioCreacion(usuario);
            partido.setEstado("ACTIVO");
        }
        partido.setFechaModificacion(ahora);
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

    private static final class Pareja {
        private int fecha;
        private UUID local;
        private UUID visita;
    }

    private static final class PartidoBorrador {
        private UUID faseId;
        private UUID grupoId;
        private UUID cruceId;
        private UUID equipoLocal;
        private UUID equipoVisita;
        private Integer posicionLocal;
        private UUID grupoLocal;
        private Integer posicionVisita;
        private UUID grupoVisita;
        private String nombre;
        private int numero;
        private LocalDate fecha;
    }
}
