package com.away.core.competition;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ClasificacionService {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");

    private final TablaService tabla;
    private final FaseRepository fases;
    private final CruceRepository cruces;
    private final PartidoRepository partidos;
    private final JornadaRepository jornadas;
    private final GrupoRepository grupos;

    public ClasificacionService(TablaService tabla, FaseRepository fases, CruceRepository cruces,
                                PartidoRepository partidos, JornadaRepository jornadas, GrupoRepository grupos) {
        this.tabla = tabla;
        this.fases = fases;
        this.cruces = cruces;
        this.partidos = partidos;
        this.jornadas = jornadas;
        this.grupos = grupos;
    }

    @Transactional(readOnly = true)
    public ClasificacionViews.CuadroView cuadro(UUID editionId, UUID categoryId, LocalDate fecha) {
        LocalDate corte = fecha == null ? LocalDate.now(GUAYAQUIL) : fecha;
        TablaViews.TablaView posiciones = tabla.listar(editionId, categoryId, corte);

        List<Fase> serie = fases.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO");
        if (serie.size() < 2) {
            return new ClasificacionViews.CuadroView(corte, List.of());
        }
        List<Fase> eliminacion = serie.subList(1, serie.size());
        Map<UUID, List<Cruce>> crucesPorFase = new HashMap<>();
        for (Cruce cruce : cruces.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            crucesPorFase.computeIfAbsent(cruce.getIdFase(), id -> new ArrayList<>()).add(cruce);
        }
        boolean hayCruces = false;
        for (Fase fase : eliminacion) {
            if (!crucesPorFase.getOrDefault(fase.getId(), List.of()).isEmpty()) {
                hayCruces = true;
                break;
            }
        }
        if (!hayCruces) {
            return new ClasificacionViews.CuadroView(corte, List.of());
        }

        Map<String, String> puestos = new HashMap<>();
        for (TablaViews.GrupoView grupo : posiciones.getGrupos()) {
            for (TablaViews.FilaView fila : grupo.getEquipos()) {
                puestos.put(grupo.getId() + ":" + fila.getPosicion(), fila.getEquipo());
            }
        }
        Map<UUID, Grupo> grupoPorId = new HashMap<>();
        for (Grupo grupo : grupos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO")) {
            grupoPorId.put(grupo.getId(), grupo);
        }
        Map<UUID, Jornada> jornadaPorId = new HashMap<>();
        for (Jornada jornada : jornadas.findByIdCategoriaAndEstadoOrderByNumeroAsc(categoryId, "ACTIVO")) {
            jornadaPorId.put(jornada.getId(), jornada);
        }
        List<Partido> partidosCategoria = partidos.findByIdCategoriaAndEstadoOrderByOrdenAsc(categoryId, "ACTIVO");

        Map<UUID, String> avanzado = new HashMap<>();
        Map<UUID, Marcador> marcadores = new HashMap<>();
        Map<UUID, Casillero> localDe = new HashMap<>();
        Map<UUID, Casillero> visitaDe = new HashMap<>();
        for (Fase fase : eliminacion) {
            for (Cruce cruce : crucesPorFase.getOrDefault(fase.getId(), List.of())) {
                Casillero local = resolver(cruce, true, puestos, grupoPorId, avanzado);
                Casillero visita = resolver(cruce, false, puestos, grupoPorId, avanzado);
                Marcador marcador = marcador(cruce, fase, partidosCategoria, jornadaPorId, corte);
                localDe.put(cruce.getId(), local);
                visitaDe.put(cruce.getId(), visita);
                marcadores.put(cruce.getId(), marcador);
                if (marcador.decidido) {
                    Casillero ganador = marcador.localGana ? local : visita;
                    if (ganador.equipo && ganador.nombre != null) {
                        avanzado.put(cruce.getId(), ganador.nombre);
                    }
                }
            }
        }

        Set<UUID> izquierda = new HashSet<>();
        Set<UUID> derecha = new HashSet<>();
        boolean primera = true;
        for (Fase fase : eliminacion) {
            List<Cruce> deFase = crucesPorFase.getOrDefault(fase.getId(), List.of());
            if (deFase.isEmpty() || deFase.size() == 1) {
                continue;
            }
            if (primera) {
                int mitad = deFase.size() / 2;
                for (int indice = 0; indice < deFase.size(); indice++) {
                    if (indice < mitad) {
                        izquierda.add(deFase.get(indice).getId());
                    } else {
                        derecha.add(deFase.get(indice).getId());
                    }
                }
                primera = false;
                continue;
            }
            List<Cruce> libres = new ArrayList<>();
            for (Cruce cruce : deFase) {
                boolean enIzquierda = izquierda.contains(cruce.getIdCruceOrigenLocal())
                        || izquierda.contains(cruce.getIdCruceOrigenVisita());
                boolean enDerecha = derecha.contains(cruce.getIdCruceOrigenLocal())
                        || derecha.contains(cruce.getIdCruceOrigenVisita());
                if (enIzquierda && !enDerecha) {
                    izquierda.add(cruce.getId());
                } else if (enDerecha && !enIzquierda) {
                    derecha.add(cruce.getId());
                } else {
                    libres.add(cruce);
                }
            }
            int mitad = libres.size() / 2;
            for (int indice = 0; indice < libres.size(); indice++) {
                if (indice < mitad) {
                    izquierda.add(libres.get(indice).getId());
                } else {
                    derecha.add(libres.get(indice).getId());
                }
            }
        }

        List<ClasificacionViews.RondaView> rondas = new ArrayList<>();
        for (Fase fase : eliminacion) {
            List<Cruce> deFase = crucesPorFase.getOrDefault(fase.getId(), List.of());
            if (deFase.isEmpty()) {
                continue;
            }
            if (deFase.size() == 1) {
                rondas.add(ronda(fase, "CENTRO", deFase, localDe, visitaDe, marcadores));
                continue;
            }
            List<Cruce> ladoIzquierdo = new ArrayList<>();
            List<Cruce> ladoDerecho = new ArrayList<>();
            for (Cruce cruce : deFase) {
                if (izquierda.contains(cruce.getId())) {
                    ladoIzquierdo.add(cruce);
                } else {
                    ladoDerecho.add(cruce);
                }
            }
            if (!ladoIzquierdo.isEmpty()) {
                rondas.add(ronda(fase, "IZQUIERDA", ladoIzquierdo, localDe, visitaDe, marcadores));
            }
            if (!ladoDerecho.isEmpty()) {
                rondas.add(ronda(fase, "DERECHA", ladoDerecho, localDe, visitaDe, marcadores));
            }
        }
        return new ClasificacionViews.CuadroView(corte, rondas);
    }

    private ClasificacionViews.RondaView ronda(Fase fase, String lado, List<Cruce> serie,
                                               Map<UUID, Casillero> localDe, Map<UUID, Casillero> visitaDe,
                                               Map<UUID, Marcador> marcadores) {
        List<ClasificacionViews.LlaveView> llaves = new ArrayList<>();
        for (Cruce cruce : serie) {
            Casillero local = localDe.get(cruce.getId());
            Casillero visita = visitaDe.get(cruce.getId());
            Marcador marcador = marcadores.get(cruce.getId());
            llaves.add(new ClasificacionViews.LlaveView(
                    cruce.getId().toString(),
                    local.nombre,
                    visita.nombre,
                    local.equipo,
                    visita.equipo,
                    marcador.completo ? marcador.golesLocal : null,
                    marcador.completo ? marcador.golesVisita : null));
        }
        return new ClasificacionViews.RondaView(fase.getNombre(), fase.getOrden(), lado, llaves);
    }

    private Casillero resolver(Cruce cruce, boolean local, Map<String, String> puestos,
                               Map<UUID, Grupo> grupoPorId, Map<UUID, String> avanzado) {
        UUID origen = local ? cruce.getIdCruceOrigenLocal() : cruce.getIdCruceOrigenVisita();
        if (origen != null) {
            String nombre = avanzado.get(origen);
            return nombre == null ? Casillero.vacio() : new Casillero(nombre, true);
        }
        UUID grupoId = local ? cruce.getIdGrupoLocal() : cruce.getIdGrupoVisita();
        Integer posicion = local ? cruce.getPosicionLocal() : cruce.getPosicionVisita();
        if (grupoId != null && posicion != null) {
            String equipo = puestos.get(grupoId + ":" + posicion);
            if (equipo != null) {
                return new Casillero(equipo, true);
            }
            return new Casillero(etiqueta(grupoPorId.get(grupoId), posicion), false);
        }
        return Casillero.vacio();
    }

    private Marcador marcador(Cruce cruce, Fase fase, List<Partido> partidos, Map<UUID, Jornada> jornadas,
                              LocalDate corte) {
        boolean idaYVuelta = "IDA_Y_VUELTA".equals(fase.getModalidad());
        int golesLocal = 0;
        int golesVisita = 0;
        int ida = 0;
        int vuelta = 0;
        for (Partido partido : partidos) {
            if (!cruce.getId().equals(partido.getIdCruce())) {
                continue;
            }
            if (partido.getGolesLocal() == null || partido.getGolesVisita() == null) {
                continue;
            }
            Jornada jornada = jornadas.get(partido.getIdJornada());
            LocalDate dia = partido.getFecha() != null ? partido.getFecha() : jornada == null ? null : jornada.getFecha();
            if (dia == null || dia.isAfter(corte)) {
                continue;
            }
            boolean esVuelta = jornada != null && jornada.getNombre() != null && jornada.getNombre().endsWith(" VUELTA");
            if (esVuelta) {
                vuelta++;
                golesLocal += partido.getGolesVisita();
                golesVisita += partido.getGolesLocal();
            } else {
                ida++;
                golesLocal += partido.getGolesLocal();
                golesVisita += partido.getGolesVisita();
            }
        }
        boolean completo = idaYVuelta ? ida > 0 && vuelta > 0 : ida > 0;
        boolean decidido = completo && golesLocal != golesVisita;
        return new Marcador(golesLocal, golesVisita, completo, decidido, golesLocal > golesVisita);
    }

    private String etiqueta(Grupo grupo, int posicion) {
        String nombre = grupo == null || grupo.getNombre() == null ? "GRUPO" : grupo.getNombre();
        return posicion + "º " + nombre;
    }

    private static final class Casillero {
        private final String nombre;
        private final boolean equipo;

        private Casillero(String nombre, boolean equipo) {
            this.nombre = nombre;
            this.equipo = equipo;
        }

        private static Casillero vacio() {
            return new Casillero(null, false);
        }
    }

    private static final class Marcador {
        private final int golesLocal;
        private final int golesVisita;
        private final boolean completo;
        private final boolean decidido;
        private final boolean localGana;

        private Marcador(int golesLocal, int golesVisita, boolean completo, boolean decidido, boolean localGana) {
            this.golesLocal = golesLocal;
            this.golesVisita = golesVisita;
            this.completo = completo;
            this.decidido = decidido;
            this.localGana = localGana;
        }
    }
}
