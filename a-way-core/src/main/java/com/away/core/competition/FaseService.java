package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FaseService {

    private static final String[] DESDE_LA_FINAL = {
            "FINAL",
            "SEMIFINAL",
            "CUARTOS DE FINAL",
            "OCTAVOS DE FINAL",
            "DIECISEISAVOS DE FINAL"
    };

    private final CategoryRepository categories;
    private final GrupoRepository grupos;
    private final GrupoEquipoRepository asignaciones;
    private final FaseRepository fases;
    private final CruceRepository cruces;
    private final AppUserRepository users;

    public FaseService(CategoryRepository categories, GrupoRepository grupos, GrupoEquipoRepository asignaciones,
                       FaseRepository fases, CruceRepository cruces, AppUserRepository users) {
        this.categories = categories;
        this.grupos = grupos;
        this.asignaciones = asignaciones;
        this.fases = fases;
        this.cruces = cruces;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public FasePanelView panel(UUID categoryId) {
        Category category = category(categoryId);
        assertRead(category.getEdition().getLeague().getId());
        return vista(category);
    }

    @Transactional
    public FasePanelView generar(UUID categoryId) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        if (!fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO").isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las fases de la categoría ya están creadas.");
        }
        List<String> nombres = nombres(category.getCantidadFases());
        String usuario = usuario();
        List<Fase> creadas = new ArrayList<>();
        int orden = 1;
        for (String nombre : nombres) {
            creadas.add(guardarFase(category.getId(), nombre, orden, usuario));
            orden++;
        }
        if (creadas.size() >= 2) {
            crearLlave(category.getId(), creadas, usuario);
        }
        return vista(category);
    }

    @Transactional
    public FasePanelView modalidad(UUID faseId, String modalidad) {
        Fase fase = fases.findById(faseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La fase no existe"));
        Category category = category(fase.getIdCategoria());
        assertWrite(category.getEdition().getLeague().getId());
        if (!"ACTIVO".equals(fase.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fase ya no está activa");
        }
        fase.setModalidad(modalidad);
        auditar(fase, usuario(), false);
        return vista(category);
    }

    @Transactional
    public FasePanelView asignar(UUID cruceId, String lado, UUID groupId, Integer posicion) {
        Cruce cruce = cruces.findById(cruceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El cruce no existe"));
        Category category = category(cruce.getIdCategoria());
        assertWrite(category.getEdition().getLeague().getId());
        if (!"ACTIVO".equals(cruce.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cruce ya no está activo");
        }
        if (cruce.getIdCruceOrigenLocal() != null || cruce.getIdCruceOrigenVisita() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este cruce se define con el ganador de la fase anterior.");
        }
        String usuario = usuario();
        if (groupId == null) {
            limpiar(cruce, lado);
            auditar(cruce, usuario, false);
            return vista(category);
        }
        if (posicion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica la posición del grupo");
        }
        Grupo grupo = grupos.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El grupo no existe"));
        if (!category.getId().equals(grupo.getIdCategoria()) || !"ACTIVO".equals(grupo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grupo no pertenece a la categoría");
        }
        int cupo = equiposEn(grupo);
        if (cupo < 1 || posicion > cupo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esa posición no existe en el grupo");
        }
        for (Cruce otro : cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            boolean local = grupo.getId().equals(otro.getIdGrupoLocal()) && posicion.equals(otro.getPosicionLocal());
            boolean visita = grupo.getId().equals(otro.getIdGrupoVisita()) && posicion.equals(otro.getPosicionVisita());
            if (!local && !visita) {
                continue;
            }
            if (local) {
                otro.setIdGrupoLocal(null);
                otro.setPosicionLocal(null);
            }
            if (visita) {
                otro.setIdGrupoVisita(null);
                otro.setPosicionVisita(null);
            }
            auditar(otro, usuario, false);
        }
        cruces.flush();
        if ("LOCAL".equals(lado)) {
            cruce.setIdGrupoLocal(grupo.getId());
            cruce.setPosicionLocal(posicion);
        } else {
            cruce.setIdGrupoVisita(grupo.getId());
            cruce.setPosicionVisita(posicion);
        }
        auditar(cruce, usuario, false);
        return vista(category);
    }

    @Transactional
    public FasePanelView eliminar(UUID categoryId) {
        Category category = category(categoryId);
        assertWrite(category.getEdition().getLeague().getId());
        String usuario = usuario();
        List<Fase> activas = fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        if (activas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no tiene fases activas");
        }
        for (Cruce cruce : cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            cruce.setEstado("ELIMINADO");
            auditar(cruce, usuario, false);
        }
        for (Fase fase : activas) {
            fase.setEstado("ELIMINADO");
            auditar(fase, usuario, false);
        }
        return vista(category);
    }

    private void crearLlave(UUID categoryId, List<Fase> creadas, String usuario) {
        int partidos = 1 << (creadas.size() - 2);
        List<UUID> anteriores = new ArrayList<>();
        for (int orden = 1; orden <= partidos; orden++) {
            anteriores.add(guardarCruce(categoryId, creadas.get(1).getId(), orden, null, null, usuario).getId());
        }
        cruces.flush();
        for (int indice = 2; indice < creadas.size(); indice++) {
            List<UUID> siguientes = new ArrayList<>();
            int cantidad = anteriores.size() / 2;
            for (int par = 0; par < cantidad; par++) {
                Cruce cruce = guardarCruce(categoryId, creadas.get(indice).getId(), par + 1,
                        anteriores.get(par * 2), anteriores.get(par * 2 + 1), usuario);
                siguientes.add(cruce.getId());
            }
            cruces.flush();
            anteriores = siguientes;
        }
    }

    private FasePanelView vista(Category category) {
        List<Grupo> activos = grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        Map<UUID, Integer> equipos = new HashMap<>();
        for (GrupoEquipo puesto : asignaciones.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            equipos.merge(puesto.getIdGrupo(), 1, Integer::sum);
        }
        List<Cruce> partidos = cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO");
        Map<UUID, Cruce> porId = new HashMap<>();
        Set<String> ocupadas = new HashSet<>();
        for (Cruce cruce : partidos) {
            porId.put(cruce.getId(), cruce);
            marcar(ocupadas, cruce.getIdGrupoLocal(), cruce.getPosicionLocal());
            marcar(ocupadas, cruce.getIdGrupoVisita(), cruce.getPosicionVisita());
        }
        Map<UUID, Grupo> porGrupo = new HashMap<>();
        List<FaseGrupoView> gruposVista = new ArrayList<>();
        List<FasePosicionView> posiciones = new ArrayList<>();
        for (Grupo grupo : activos) {
            porGrupo.put(grupo.getId(), grupo);
            gruposVista.add(new FaseGrupoView(grupo.getId(), grupo.getNombre()));
            int cupo = equipos.getOrDefault(grupo.getId(), 0);
            for (int posicion = 1; posicion <= cupo; posicion++) {
                String clave = clave(grupo.getId(), posicion);
                posiciones.add(new FasePosicionView(grupo.getId(), posicion, etiqueta(grupo, posicion), ocupadas.contains(clave)));
            }
        }
        for (Cruce cruce : partidos) {
            asegurar(porGrupo, cruce.getIdGrupoLocal());
            asegurar(porGrupo, cruce.getIdGrupoVisita());
        }
        Map<UUID, List<CruceView>> porFase = new HashMap<>();
        for (Cruce cruce : partidos) {
            boolean editable = cruce.getIdCruceOrigenLocal() == null && cruce.getIdCruceOrigenVisita() == null;
            porFase.computeIfAbsent(cruce.getIdFase(), key -> new ArrayList<>())
                    .add(new CruceView(cruce.getId(), cruce.getOrden(), editable,
                            cruce.getIdGrupoLocal(), cruce.getPosicionLocal(), texto(cruce, true, porId, porGrupo),
                            cruce.getIdGrupoVisita(), cruce.getPosicionVisita(), texto(cruce, false, porId, porGrupo)));
        }
        List<FaseView> vistas = new ArrayList<>();
        for (Fase fase : fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(category.getId(), "ACTIVO")) {
            vistas.add(new FaseView(fase.getId(), fase.getNombre(), fase.getOrden(), fase.getModalidad(),
                    porFase.getOrDefault(fase.getId(), List.of())));
        }
        return new FasePanelView(category.getId(), category.getName(), category.getCantidadFases(), gruposVista, posiciones, vistas);
    }

    private void asegurar(Map<UUID, Grupo> porGrupo, UUID groupId) {
        if (groupId != null && !porGrupo.containsKey(groupId)) {
            grupos.findById(groupId).ifPresent(grupo -> porGrupo.put(grupo.getId(), grupo));
        }
    }

    private String texto(Cruce cruce, boolean local, Map<UUID, Cruce> porId, Map<UUID, Grupo> porGrupo) {
        Integer posicion = local ? cruce.getPosicionLocal() : cruce.getPosicionVisita();
        UUID groupId = local ? cruce.getIdGrupoLocal() : cruce.getIdGrupoVisita();
        if (posicion != null && groupId != null) {
            return etiqueta(porGrupo.get(groupId), posicion);
        }
        UUID origenId = local ? cruce.getIdCruceOrigenLocal() : cruce.getIdCruceOrigenVisita();
        if (origenId == null) {
            return null;
        }
        Cruce origen = porId.get(origenId);
        return origen == null ? null : avanza(origen, porId, porGrupo);
    }

    private String avanza(Cruce cruce, Map<UUID, Cruce> porId, Map<UUID, Grupo> porGrupo) {
        String local = texto(cruce, true, porId, porGrupo);
        String visita = texto(cruce, false, porId, porGrupo);
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

    private int equiposEn(Grupo grupo) {
        return asignaciones.findByIdGrupoAndEstado(grupo.getId(), "ACTIVO").size();
    }

    private void limpiar(Cruce cruce, String lado) {
        if ("LOCAL".equals(lado)) {
            cruce.setIdGrupoLocal(null);
            cruce.setPosicionLocal(null);
        } else {
            cruce.setIdGrupoVisita(null);
            cruce.setPosicionVisita(null);
        }
    }

    private void marcar(Set<String> ocupadas, UUID groupId, Integer posicion) {
        if (groupId != null && posicion != null) {
            ocupadas.add(clave(groupId, posicion));
        }
    }

    private String clave(UUID groupId, int posicion) {
        return groupId + ":" + posicion;
    }

    private static final String[] ORDINALES = {
            "primero", "segundo", "tercero", "cuarto", "quinto",
            "sexto", "séptimo", "octavo", "noveno", "décimo",
            "undécimo", "duodécimo", "decimotercero", "decimocuarto", "decimoquinto",
            "decimosexto", "decimoséptimo", "decimoctavo", "decimonoveno", "vigésimo"
    };

    private String etiqueta(Grupo grupo, int posicion) {
        String nombre = grupo == null ? "GRUPO" : grupo.getNombre();
        String ordinal = posicion >= 1 && posicion <= ORDINALES.length ? ORDINALES[posicion - 1] : posicion + "º";
        return (ordinal + " " + nombre).toUpperCase(java.util.Locale.forLanguageTag("es"));
    }

    private List<String> nombres(int cantidad) {
        if (cantidad < 1 || cantidad > DESDE_LA_FINAL.length + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría admite como máximo 6 fases (hasta dieciseisavos).");
        }
        List<String> nombres = new ArrayList<>();
        nombres.add("FASE DE GRUPOS");
        for (int indice = cantidad - 2; indice >= 0; indice--) {
            nombres.add(DESDE_LA_FINAL[indice]);
        }
        return nombres;
    }

    private Fase guardarFase(UUID categoryId, String nombre, int orden, String usuario) {
        Fase fase = new Fase();
        fase.setId(UUID.randomUUID());
        fase.setIdCategoria(categoryId);
        fase.setNombre(nombre);
        fase.setOrden(orden);
        fase.setModalidad("IDA");
        auditar(fase, usuario, true);
        return fases.save(fase);
    }

    private Cruce guardarCruce(UUID categoryId, UUID faseId, int orden, UUID origenLocal, UUID origenVisita, String usuario) {
        Cruce cruce = new Cruce();
        cruce.setId(UUID.randomUUID());
        cruce.setIdFase(faseId);
        cruce.setIdCategoria(categoryId);
        cruce.setOrden(orden);
        cruce.setIdCruceOrigenLocal(origenLocal);
        cruce.setIdCruceOrigenVisita(origenVisita);
        auditar(cruce, usuario, true);
        return cruces.save(cruce);
    }

    private Category category(UUID id) {
        return categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));
    }

    private void auditar(Fase fase, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            fase.setFechaCreacion(ahora);
            fase.setUsuarioCreacion(usuario);
            fase.setEstado("ACTIVO");
        }
        fase.setFechaModificacion(ahora);
        fase.setUsuarioModificacion(usuario);
    }

    private void auditar(Cruce cruce, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            cruce.setFechaCreacion(ahora);
            cruce.setUsuarioCreacion(usuario);
            cruce.setEstado("ACTIVO");
        }
        cruce.setFechaModificacion(ahora);
        cruce.setUsuarioModificacion(usuario);
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
