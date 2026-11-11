package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class GrupoService {

    private final CategoryRepository categories;
    private final TeamEntryRepository entries;
    private final GrupoRepository grupos;
    private final GrupoEquipoRepository asignaciones;
    private final AppUserRepository users;

    public GrupoService(CategoryRepository categories, TeamEntryRepository entries,
                        GrupoRepository grupos, GrupoEquipoRepository asignaciones, AppUserRepository users) {
        this.categories = categories;
        this.entries = entries;
        this.grupos = grupos;
        this.asignaciones = asignaciones;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public GrupoPanelView panel(UUID categoryId) {
        Category category = category(categoryId);
        assertRead(category.getEdition().getLeague().getId());
        return vista(category);
    }

    @Transactional
    public GrupoPanelView generar(UUID categoryId) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        List<Grupo> actuales = activos(category.getId());
        Set<String> nombres = new HashSet<>();
        int orden = 0;
        for (Grupo grupo : actuales) {
            nombres.add(grupo.getNombre());
            orden = Math.max(orden, grupo.getOrden());
        }
        boolean creo = false;
        for (String nombre : nombresEsperados(category.getCantidadGrupos())) {
            if (nombres.contains(nombre)) {
                continue;
            }
            orden++;
            guardarGrupo(category.getId(), nombre, orden);
            creo = true;
        }
        if (!creo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los grupos de la categoría ya están creados.");
        }
        return vista(category);
    }

    @Transactional
    public GrupoPanelView agregar(UUID categoryId) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        List<Grupo> actuales = activos(category.getId());
        Set<String> nombres = new HashSet<>();
        int orden = 0;
        for (Grupo grupo : actuales) {
            nombres.add(grupo.getNombre());
            orden = Math.max(orden, grupo.getOrden());
        }
        int indice = 0;
        String nombre = "GRUPO " + letra(indice);
        while (nombres.contains(nombre)) {
            indice++;
            nombre = "GRUPO " + letra(indice);
        }
        guardarGrupo(category.getId(), nombre, orden + 1);
        return vista(category);
    }

    @Transactional
    public GrupoPanelView eliminar(UUID groupId) {
        Grupo grupo = grupos.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El grupo no existe"));
        Category category = category(grupo.getIdCategoria());
        assertWrite(category.getEdition().getLeague().getId());
        if (!"ACTIVO".equals(grupo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grupo ya no está activo");
        }
        String usuario = usuario();
        grupo.setEstado("ELIMINADO");
        auditar(grupo, usuario, false);
        for (GrupoEquipo asignacion : asignaciones.findByIdGrupoAndEstado(grupo.getId(), "ACTIVO")) {
            asignacion.setEstado("ELIMINADO");
            auditar(asignacion, usuario, false);
        }
        return vista(category);
    }

    @Transactional
    public GrupoPanelView asignarAutomatico(UUID categoryId) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        List<Grupo> actuales = activos(category.getId());
        if (actuales.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crea los grupos antes de asignar equipos.");
        }
        List<Team> libres = libres(category.getId());
        Map<UUID, Integer> conteo = conteo(category.getId(), actuales);
        Integer limite = category.getEquiposPorGrupo();
        String usuario = usuario();
        while (!libres.isEmpty()) {
            Grupo destino = actuales.stream()
                    .filter(grupo -> limite == null || conteo.get(grupo.getId()) < limite)
                    .min(Comparator.comparingInt((Grupo grupo) -> conteo.get(grupo.getId())).thenComparingInt(Grupo::getOrden))
                    .orElse(null);
            if (destino == null) {
                break;
            }
            Team equipo = libres.remove(0);
            int orden = conteo.get(destino.getId()) + 1;
            asignar(category.getId(), destino.getId(), equipo, orden, usuario);
            conteo.put(destino.getId(), orden);
        }
        return vista(category);
    }

    @Transactional
    public GrupoPanelView mover(UUID categoryId, UUID teamId, UUID groupId) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        Team equipo = equipoInscrito(category, teamId);
        Grupo destino = null;
        if (groupId != null) {
            destino = grupos.findById(groupId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El grupo no existe"));
            if (!category.getId().equals(destino.getIdCategoria()) || !"ACTIVO".equals(destino.getEstado())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grupo no pertenece a la categoría");
            }
        }
        String usuario = usuario();
        GrupoEquipo actual = asignacionActiva(category.getId(), teamId);
        if (actual != null && destino != null && destino.getId().equals(actual.getIdGrupo())) {
            return vista(category);
        }
        if (actual != null) {
            actual.setEstado("ELIMINADO");
            auditar(actual, usuario, false);
            asignaciones.flush();
        }
        if (destino != null) {
            int orden = (int) asignaciones.findByIdGrupoAndEstado(destino.getId(), "ACTIVO").stream()
                    .filter(item -> "ACTIVO".equals(item.getEstado()))
                    .count() + 1;
            asignar(category.getId(), destino.getId(), equipo, orden, usuario);
        }
        return vista(category);
    }

    private GrupoPanelView vista(Category category) {
        List<Grupo> actuales = activos(category.getId());
        List<GrupoEquipo> puestos = asignaciones.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        Map<UUID, List<GrupoEquipoView>> porGrupo = new HashMap<>();
        Set<UUID> ocupados = new HashSet<>();
        for (GrupoEquipo puesto : puestos) {
            ocupados.add(puesto.getEquipo().getId());
            porGrupo.computeIfAbsent(puesto.getIdGrupo(), key -> new ArrayList<>())
                    .add(new GrupoEquipoView(puesto.getEquipo().getId(), puesto.getEquipo().getName()));
        }
        List<GrupoView> vistas = new ArrayList<>();
        for (Grupo grupo : actuales) {
            vistas.add(new GrupoView(grupo.getId(), grupo.getNombre(), grupo.getOrden(),
                    porGrupo.getOrDefault(grupo.getId(), List.of())));
        }
        List<GrupoEquipoView> sinGrupo = new ArrayList<>();
        for (TeamEntry entry : entries.findByCategory_IdOrderByTeam_NameAsc(category.getId())) {
            Team equipo = entry.getTeam();
            if (!"ACTIVO".equals(equipo.getEstado()) || ocupados.contains(equipo.getId())) {
                continue;
            }
            sinGrupo.add(new GrupoEquipoView(equipo.getId(), equipo.getName()));
        }
        return new GrupoPanelView(category.getId(), category.getName(), category.getCantidadGrupos(),
                category.getEquiposPorGrupo(), vistas, sinGrupo);
    }

    private List<Team> libres(UUID categoryId) {
        Set<UUID> ocupados = new HashSet<>();
        for (GrupoEquipo puesto : asignaciones.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            ocupados.add(puesto.getEquipo().getId());
        }
        List<Team> libres = new ArrayList<>();
        for (TeamEntry entry : entries.findByCategory_IdOrderByTeam_NameAsc(categoryId)) {
            Team equipo = entry.getTeam();
            if ("ACTIVO".equals(equipo.getEstado()) && !ocupados.contains(equipo.getId())) {
                libres.add(equipo);
            }
        }
        return libres;
    }

    private Map<UUID, Integer> conteo(UUID categoryId, List<Grupo> actuales) {
        Map<UUID, Integer> conteo = new HashMap<>();
        for (Grupo grupo : actuales) {
            conteo.put(grupo.getId(), 0);
        }
        for (GrupoEquipo puesto : asignaciones.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            conteo.computeIfPresent(puesto.getIdGrupo(), (key, value) -> value + 1);
        }
        return conteo;
    }

    private GrupoEquipo asignacionActiva(UUID categoryId, UUID teamId) {
        for (GrupoEquipo puesto : asignaciones.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            if (puesto.getEquipo().getId().equals(teamId)) {
                return puesto;
            }
        }
        return null;
    }

    private Team equipoInscrito(Category category, UUID teamId) {
        TeamEntry entry = entries.findByTeam_IdAndCategory_Id(teamId, category.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no está inscrito en la categoría"));
        Team equipo = entry.getTeam();
        if (!"ACTIVO".equals(equipo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede agrupar un equipo activo");
        }
        return equipo;
    }

    private void asignar(UUID categoryId, UUID groupId, Team equipo, int orden, String usuario) {
        GrupoEquipo nuevo = new GrupoEquipo();
        nuevo.setId(UUID.randomUUID());
        nuevo.setIdGrupo(groupId);
        nuevo.setIdCategoria(categoryId);
        nuevo.setEquipo(equipo);
        nuevo.setOrden(orden);
        auditar(nuevo, usuario, true);
        asignaciones.save(nuevo);
    }

    private void guardarGrupo(UUID categoryId, String nombre, int orden) {
        Grupo grupo = new Grupo();
        grupo.setId(UUID.randomUUID());
        grupo.setIdCategoria(categoryId);
        grupo.setNombre(nombre);
        grupo.setOrden(orden);
        auditar(grupo, usuario(), true);
        grupos.save(grupo);
    }

    private List<Grupo> activos(UUID categoryId) {
        return grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO");
    }

    private List<String> nombresEsperados(int cantidad) {
        List<String> nombres = new ArrayList<>();
        if (cantidad <= 1) {
            nombres.add("GRUPO GENERAL");
            return nombres;
        }
        for (int indice = 0; indice < cantidad; indice++) {
            nombres.add("GRUPO " + letra(indice));
        }
        return nombres;
    }

    private String letra(int indice) {
        StringBuilder texto = new StringBuilder();
        int valor = indice;
        do {
            texto.insert(0, (char) ('A' + valor % 26));
            valor = valor / 26 - 1;
        } while (valor >= 0);
        return texto.toString();
    }

    private Category category(UUID id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
    }

    private void auditar(Grupo grupo, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            grupo.setFechaCreacion(ahora);
            grupo.setUsuarioCreacion(usuario);
            grupo.setEstado("ACTIVO");
        }
        grupo.setFechaModificacion(ahora);
        grupo.setUsuarioModificacion(usuario);
    }

    private void auditar(GrupoEquipo asignacion, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            asignacion.setFechaCreacion(ahora);
            asignacion.setUsuarioCreacion(usuario);
            asignacion.setEstado("ACTIVO");
        }
        asignacion.setFechaModificacion(ahora);
        asignacion.setUsuarioModificacion(usuario);
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
}
