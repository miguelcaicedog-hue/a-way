package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RefuerzoService {

    private static final String EXISTENTE = "EXISTENTE";

    private final RefuerzoArchivoRepository archivos;
    private final RefuerzoEquipoRepository equipos;
    private final RefuerzoJugadorRepository jugadores;
    private final EditionRepository ediciones;
    private final CategoryRepository categorias;
    private final TeamRepository teams;
    private final TeamEntryRepository entries;
    private final PlayerRepository players;
    private final AppUserRepository users;
    private final TransactionTemplate transactions;

    public RefuerzoService(RefuerzoArchivoRepository archivos,
                           RefuerzoEquipoRepository equipos,
                           RefuerzoJugadorRepository jugadores,
                           EditionRepository ediciones,
                           CategoryRepository categorias,
                           TeamRepository teams,
                           TeamEntryRepository entries,
                           PlayerRepository players,
                           AppUserRepository users,
                           PlatformTransactionManager transactions) {
        this.archivos = archivos;
        this.equipos = equipos;
        this.jugadores = jugadores;
        this.ediciones = ediciones;
        this.categorias = categorias;
        this.teams = teams;
        this.entries = entries;
        this.players = players;
        this.users = users;
        this.transactions = new TransactionTemplate(transactions);
    }

    @Transactional(readOnly = true)
    public List<RefuerzoArchivoView> listar(UUID leagueId, UUID editionId) {
        assertRead(leagueId);
        exigirEdicion(leagueId, editionId);
        return archivos.findByIdLigaAndIdEdicionAndEstadoOrderByFechaCreacionDesc(leagueId, editionId, "ACTIVO").stream()
                .map(this::archivoView)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminar(UUID archivoId) {
        RefuerzoArchivo archivo = archivo(archivoId);
        assertWrite(archivo.getIdLiga());
        List<RefuerzoEquipo> cargados = equipos.findByIdRefuerzoArchivoAndEstadoOrderByNumeroFilaAsc(archivo.getId(), "ACTIVO");
        boolean procesado = cargados.stream().anyMatch(equipo -> "OK".equals(equipo.getResultado()));
        if (procesado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo ya fue procesado");
        }
        String usuario = usuario();
        for (RefuerzoEquipo equipo : cargados) {
            for (RefuerzoJugador jugador : jugadores.findByIdRefuerzoEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO")) {
                jugador.setEstado("ELIMINADO");
                auditar(jugador, usuario, false);
            }
            equipo.setEstado("ELIMINADO");
            auditar(equipo, usuario, false);
        }
        archivo.setEstado("ELIMINADO");
        auditar(archivo, usuario, false);
    }

    @Transactional(readOnly = true)
    public RefuerzoDetalleView detalle(UUID archivoId) {
        RefuerzoArchivo archivo = archivo(archivoId);
        assertRead(archivo.getIdLiga());
        return detalleView(archivo);
    }

    @Transactional(readOnly = true)
    public List<RefuerzoJugadorView> jugadores(UUID equipoId) {
        RefuerzoEquipo equipo = equipo(equipoId);
        RefuerzoArchivo archivo = archivo(equipo.getIdRefuerzoArchivo());
        assertRead(archivo.getIdLiga());
        return jugadores.findByIdRefuerzoEquipoAndEstadoOrderByOrdenAsc(equipoId, "ACTIVO").stream()
                .map(this::jugadorView)
                .collect(Collectors.toList());
    }

    @Transactional
    public RefuerzoDetalleView cargar(UUID leagueId, UUID editionId, MultipartFile archivo) {
        assertWrite(leagueId);
        exigirEdicion(leagueId, editionId);
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un archivo de Excel");
        }
        String nombre = archivo.getOriginalFilename() == null ? "refuerzos.xlsx" : archivo.getOriginalFilename();
        if (!nombre.toLowerCase().endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser .xlsx");
        }
        List<RefuerzoWorkbook.EquipoLeido> leidos;
        try {
            leidos = RefuerzoWorkbook.leer(archivo.getInputStream());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo de Excel");
        }
        if (leidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no tiene equipos");
        }
        String usuario = usuario();
        RefuerzoArchivo guardado = new RefuerzoArchivo();
        guardado.setId(UUID.randomUUID());
        guardado.setIdLiga(leagueId);
        guardado.setIdEdicion(editionId);
        guardado.setNombreArchivo(limitar(nombre, 255));
        auditar(guardado, usuario, true);
        archivos.save(guardado);
        for (RefuerzoWorkbook.EquipoLeido leido : leidos) {
            RefuerzoEquipo equipo = new RefuerzoEquipo();
            equipo.setId(UUID.randomUUID());
            equipo.setIdRefuerzoArchivo(guardado.getId());
            equipo.setNumeroFila(leido.fila);
            equipo.setCategoria(leido.categoria == null ? "" : leido.categoria);
            equipo.setNombreEquipo(leido.nombre);
            equipo.setSeleccionado(true);
            auditar(equipo, usuario, true);
            equipos.save(equipo);
            for (RefuerzoWorkbook.JugadorLeido jugadorLeido : leido.jugadores) {
                RefuerzoJugador jugador = new RefuerzoJugador();
                jugador.setId(UUID.randomUUID());
                jugador.setIdRefuerzoEquipo(equipo.getId());
                jugador.setOrden(jugadorLeido.orden);
                jugador.setNumeroCamisaReemplazo(jugadorLeido.numeroCamisaReemplazo);
                jugador.setCedula(jugadorLeido.cedula);
                jugador.setNombre(jugadorLeido.nombre);
                jugador.setNumeroCamisa(jugadorLeido.numeroCamisa);
                jugador.setUrlFoto(jugadorLeido.urlFoto);
                jugador.setCargado(false);
                jugador.setExistente(false);
                jugador.setFotoCargada(false);
                auditar(jugador, usuario, true);
                jugadores.save(jugador);
            }
        }
        return detalleView(guardado);
    }

    @Transactional
    public RefuerzoEquipoView seleccionar(UUID equipoId, boolean seleccionado) {
        RefuerzoEquipo equipo = equipo(equipoId);
        RefuerzoArchivo archivo = archivo(equipo.getIdRefuerzoArchivo());
        assertWrite(archivo.getIdLiga());
        equipo.setSeleccionado(seleccionado);
        auditar(equipo, usuario(), false);
        return equipoView(equipos.save(equipo));
    }

    @Transactional
    public RefuerzoEquipoView renombrar(UUID equipoId, String nombre) {
        RefuerzoEquipo equipo = equipo(equipoId);
        RefuerzoArchivo archivo = archivo(equipo.getIdRefuerzoArchivo());
        assertWrite(archivo.getIdLiga());
        if ("OK".equals(equipo.getResultado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo ya fue procesado");
        }
        boolean cargado = jugadores.findByIdRefuerzoEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO").stream()
                .anyMatch(RefuerzoJugador::isCargado);
        if (cargado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo ya tiene jugadores procesados");
        }
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty() || limpio.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del equipo debe tener hasta 160 caracteres");
        }
        equipo.setNombreEquipo(limpio);
        equipo.setResultado(null);
        equipo.setDetalle(null);
        auditar(equipo, usuario(), false);
        return equipoView(equipos.save(equipo));
    }

    public RefuerzoDetalleView procesar(UUID archivoId, List<UUID> equipoIds) {
        RefuerzoArchivo archivo = archivo(archivoId);
        assertWrite(archivo.getIdLiga());
        Set<UUID> elegidos = new HashSet<>(equipoIds == null ? List.of() : equipoIds);
        String usuario = usuario();
        transactions.executeWithoutResult(status -> {
            for (RefuerzoEquipo equipo : filas(archivoId)) {
                if ("OK".equals(equipo.getResultado())) {
                    continue;
                }
                equipo.setSeleccionado(elegidos.contains(equipo.getId()));
                auditar(equipo, usuario, false);
            }
        });
        for (UUID equipoId : elegidos) {
            transactions.executeWithoutResult(status -> procesarEquipo(archivoId, equipoId, usuario));
        }
        return detalle(archivoId);
    }

    public AccesoFotosView accesoFotos(UUID archivoId, List<UUID> equipoIds) {
        RefuerzoArchivo archivo = archivo(archivoId);
        assertWrite(archivo.getIdLiga());
        Set<UUID> elegidos = new HashSet<>(equipoIds == null ? List.of() : equipoIds);
        List<String> muestras = new ArrayList<>();
        for (RefuerzoEquipo equipo : filas(archivoId)) {
            if (!elegidos.contains(equipo.getId()) || "OK".equals(equipo.getResultado())) {
                continue;
            }
            for (RefuerzoJugador jugador : jugadores.findByIdRefuerzoEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO")) {
                if (jugador.isFotoCargada() || jugador.getUrlFoto() == null || jugador.getUrlFoto().isBlank()) {
                    continue;
                }
                muestras.add(jugador.getUrlFoto());
                if (muestras.size() == 3) {
                    break;
                }
            }
            if (muestras.size() == 3) {
                break;
            }
        }
        if (muestras.isEmpty()) {
            return new AccesoFotosView(true, null);
        }
        for (String url : muestras) {
            if (FotoInscripcion.descargar(url).error == null) {
                return new AccesoFotosView(true, null);
            }
        }
        return new AccesoFotosView(false,
                "No hay acceso a las fotos de Google Drive. Si continúas, los jugadores se cargarán y las fotos que no se puedan descargar quedarán para revisar.");
    }

    public RefuerzoEquipoView procesarUno(UUID equipoId) {
        RefuerzoEquipo equipo = equipo(equipoId);
        RefuerzoArchivo archivo = archivo(equipo.getIdRefuerzoArchivo());
        assertWrite(archivo.getIdLiga());
        String usuario = usuario();
        transactions.executeWithoutResult(status -> procesarEquipo(archivo.getId(), equipoId, usuario));
        return equipoView(equipos.findById(equipoId).orElseThrow());
    }

    private void procesarEquipo(UUID archivoId, UUID equipoId, String usuario) {
        RefuerzoEquipo equipo = equipos.findById(equipoId).orElse(null);
        if (equipo == null || !archivoId.equals(equipo.getIdRefuerzoArchivo()) || "OK".equals(equipo.getResultado())) {
            return;
        }
        RefuerzoArchivo archivo = archivo(archivoId);
        List<RefuerzoJugador> plantel = jugadores.findByIdRefuerzoEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO");
        if (equipo.getCategoria() == null || equipo.getCategoria().isBlank()) {
            cerrar(equipo, "SIN CARGA", "La fila no tiene categoría.", usuario);
            return;
        }
        if (plantel.isEmpty()) {
            cerrar(equipo, "SIN CARGA", "El equipo no tiene jugadores para cargar.", usuario);
            return;
        }
        Category categoria = categoria(archivo, equipo.getCategoria());
        if (categoria == null) {
            cerrar(equipo, "SIN CARGA", "No existe la categoría " + equipo.getCategoria().trim() + " en la edición seleccionada.", usuario);
            return;
        }
        if (equipo.getNombreEquipo() == null || equipo.getNombreEquipo().isBlank()) {
            cerrar(equipo, "SIN CARGA", "El nombre del equipo no se puede guardar.", usuario);
            return;
        }
        Team team = teams.findByLeague_IdAndNameAndEstadoNot(archivo.getIdLiga(), equipo.getNombreEquipo().trim(), "ELIMINADO").orElse(null);
        if (team == null) {
            cerrar(equipo, "SIN CARGA", "El equipo no está inscrito en la liga.", usuario);
            return;
        }
        if (!"ACTIVO".equals(team.getEstado())) {
            cerrar(equipo, "SIN CARGA", "El equipo no está activo.", usuario);
            return;
        }
        TeamEntry entry = entries.findByTeam_IdAndCategory_Id(team.getId(), categoria.getId()).orElse(null);
        if (entry == null) {
            cerrar(equipo, "SIN CARGA", "El equipo no está inscrito en la categoría " + categoria.getName() + ".", usuario);
            return;
        }
        List<Player> roster = new ArrayList<>(players.findByTeamEntry_IdOrderByCedulaAsc(entry.getId()));
        for (RefuerzoJugador jugador : plantel) {
            auditar(jugador, usuario, false);
            if (jugador.isCargado()) {
                reintentarFoto(jugador, roster);
                continue;
            }
            jugador.setExistente(false);
            jugador.setMotivo(null);
            aplicarJugador(categoria, entry, roster, jugador);
        }
        cerrarPlantel(equipo, plantel, usuario);
        jugadores.saveAll(plantel);
    }

    private void aplicarJugador(Category categoria, TeamEntry entry, List<Player> roster, RefuerzoJugador jugador) {
        String cedula = jugador.getCedula() == null ? "" : jugador.getCedula().trim();
        String nombre = jugador.getNombre() == null ? "" : jugador.getNombre().trim();
        Integer numero = camisa(jugador.getNumeroCamisa());
        Integer reemplazo = camisa(jugador.getNumeroCamisaReemplazo());
        boolean pideReemplazo = jugador.getNumeroCamisaReemplazo() != null && !jugador.getNumeroCamisaReemplazo().isBlank();
        if (cedula.isEmpty() || cedula.length() > 30) {
            jugador.setMotivo(etiqueta(jugador) + "revisar la cédula.");
            return;
        }
        if (nombre.isEmpty()) {
            jugador.setMotivo(etiqueta(jugador) + "falta el nombre.");
            return;
        }
        if (numero == null) {
            jugador.setMotivo(etiqueta(jugador) + "el número de camisa no es válido.");
            return;
        }
        if (pideReemplazo && reemplazo == null) {
            jugador.setMotivo(etiqueta(jugador) + "el número de camiseta a reemplazar no es válido.");
            return;
        }
        if (roster.stream().anyMatch(item -> item.getCedula().equalsIgnoreCase(cedula))) {
            jugador.setExistente(true);
            jugador.setCargado(false);
            jugador.setMotivo(EXISTENTE);
            return;
        }
        Player sale = null;
        if (pideReemplazo) {
            sale = roster.stream()
                    .filter(item -> "ACTIVO".equals(item.getEstado()) && item.getNumeroCamisa() == reemplazo)
                    .findFirst()
                    .orElse(null);
            if (sale == null) {
                jugador.setMotivo(etiqueta(jugador) + "el número de camiseta a reemplazar no existe en el equipo.");
                return;
            }
        } else if (activos(roster) >= categoria.getMaxPlayers()) {
            jugador.setMotivo(etiqueta(jugador) + "no hay cupo disponible en el equipo.");
            return;
        }
        if (camisaOcupada(roster, numero, sale == null ? null : sale.getId())) {
            jugador.setMotivo(etiqueta(jugador) + "el número de camisa ya está usado en el equipo.");
            return;
        }
        for (Player otro : players.findByCategoriaAndCedula(categoria.getId(), cedula)) {
            if (otro.getTeamEntry().getId().equals(entry.getId()) || !"ACTIVO".equals(otro.getEstado())) {
                continue;
            }
            otro.setEstado("INACTIVO");
            players.saveAndFlush(otro);
        }
        if (sale != null) {
            sale.setEstado("INACTIVO");
            players.saveAndFlush(sale);
        }
        Player player = new Player();
        player.setId(UUID.randomUUID());
        player.setTeamEntry(entry);
        player.setName(nombre);
        player.setCedula(cedula);
        player.setNumeroCamisa(numero);
        player.setAlias(aliasDe(jugador, nombre));
        player.setRotationDegrees((short) 0);
        player.setEstado("ACTIVO");
        player.setHabilitado(false);
        player.setReemplazo(true);
        players.save(player);
        jugador.setCargado(true);
        jugador.setExistente(false);
        jugador.setAlias(player.getAlias());
        jugador.setMotivo(null);
        roster.add(player);
        if (esperaFoto(jugador)) {
            aplicarFoto(jugador, player);
        }
    }

    private void reintentarFoto(RefuerzoJugador jugador, List<Player> roster) {
        if (jugador.isFotoCargada() || !esperaFoto(jugador)) {
            return;
        }
        Player guardado = roster.stream()
                .filter(item -> item.getCedula().equalsIgnoreCase(jugador.getCedula() == null ? "" : jugador.getCedula().trim()))
                .findFirst()
                .orElse(null);
        if (guardado != null) {
            aplicarFoto(jugador, guardado);
        }
    }

    private void cerrarPlantel(RefuerzoEquipo equipo, List<RefuerzoJugador> plantel, String usuario) {
        List<String> avisos = new ArrayList<>();
        boolean alguno = false;
        boolean pendiente = false;
        for (RefuerzoJugador jugador : plantel) {
            if (jugador.isCargado()) {
                alguno = true;
            }
            if (!jugador.isExistente() && (!jugador.isCargado() || esperaFoto(jugador))) {
                pendiente = true;
            }
            String motivo = jugador.getMotivo();
            if (motivo != null && !motivo.isBlank() && !EXISTENTE.equals(motivo)) {
                avisos.add(motivo);
            }
        }
        if (!alguno && plantel.stream().anyMatch(jugador -> !jugador.isExistente())) {
            avisos.add(0, "No se cargó ningún jugador.");
        }
        String detalle = avisos.stream().filter(aviso -> aviso != null && !aviso.isBlank()).collect(Collectors.joining("\n"));
        if (!pendiente && detalle.isBlank()) {
            cerrar(equipo, "OK", null, usuario);
        } else {
            cerrar(equipo, "ERROR", detalle.isBlank() ? null : detalle, usuario);
        }
    }

    private void aplicarFoto(RefuerzoJugador jugador, Player player) {
        if (tieneFoto(jugador)) {
            player.setPhoto(jugador.getFoto());
            player.setPhotoContentType(jugador.getTipoFoto() == null ? "image/jpeg" : jugador.getTipoFoto());
            players.save(player);
            jugador.setFotoCargada(true);
            jugador.setMotivo(null);
            return;
        }
        FotoInscripcion.Descarga descarga = FotoInscripcion.descargar(jugador.getUrlFoto());
        if (descarga.error != null) {
            jugador.setFotoCargada(false);
            jugador.setMotivo(etiqueta(jugador) + descarga.error + ".");
            return;
        }
        player.setPhoto(descarga.bytes);
        player.setPhotoContentType(descarga.contentType);
        players.save(player);
        jugador.setFotoCargada(true);
        jugador.setMotivo(null);
    }

    @Transactional
    public RefuerzoJugadorView editarJugador(UUID jugadorId, String cedula, String nombre, String alias,
                                             String numeroCamisa, String numeroCamisaReemplazo, MultipartFile foto) {
        RefuerzoJugador jugador = jugadores.findById(jugadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado"));
        if (!"ACTIVO".equals(jugador.getEstado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado");
        }
        RefuerzoEquipo equipo = equipo(jugador.getIdRefuerzoEquipo());
        RefuerzoArchivo archivo = archivo(equipo.getIdRefuerzoArchivo());
        assertWrite(archivo.getIdLiga());
        String cedulaNueva = cedula == null ? "" : cedula.trim();
        String nombreNuevo = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        String camisaNueva = numeroCamisa == null ? "" : numeroCamisa.trim();
        String reemplazoNuevo = numeroCamisaReemplazo == null ? "" : numeroCamisaReemplazo.trim();
        if (cedulaNueva.isEmpty() || cedulaNueva.length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cédula debe tener hasta 30 caracteres");
        }
        if (nombreNuevo.isEmpty() || nombreNuevo.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        Integer numero = camisa(camisaNueva);
        if (numero == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de camisa debe ser un entero igual o mayor que cero");
        }
        if (!reemplazoNuevo.isEmpty() && camisa(reemplazoNuevo) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de camiseta a reemplazar debe ser un entero igual o mayor que cero");
        }
        String aliasNuevo = alias == null ? "" : alias.trim().replaceAll("\\s+", " ");
        if (aliasNuevo.isEmpty()) {
            aliasNuevo = aliasDesdeNombre(nombreNuevo);
        }
        if (aliasNuevo != null && aliasNuevo.length() > 80) {
            aliasNuevo = aliasNuevo.substring(0, 80);
        }
        byte[] fotoNueva = null;
        String tipoFoto = null;
        if (foto != null && !foto.isEmpty()) {
            try {
                FotoInscripcion.Descarga descarga = FotoInscripcion.preparar(foto.getBytes(), foto.getContentType());
                if (descarga.error != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, descarga.error);
                }
                fotoNueva = descarga.bytes;
                tipoFoto = descarga.contentType;
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto no se pudo leer");
            }
        }
        String cedulaAnterior = jugador.getCedula() == null ? "" : jugador.getCedula().trim();
        boolean estabaCargado = jugador.isCargado();
        jugador.setCedula(cedulaNueva);
        jugador.setNombre(limitar(nombreNuevo, 200));
        jugador.setNumeroCamisa(String.valueOf(numero));
        jugador.setNumeroCamisaReemplazo(reemplazoNuevo.isEmpty() ? null : reemplazoNuevo);
        jugador.setAlias(aliasNuevo);
        if (fotoNueva != null) {
            jugador.setFoto(fotoNueva);
            jugador.setTipoFoto(tipoFoto);
            jugador.setFotoCargada(false);
        }
        String usuario = usuario();
        auditar(jugador, usuario, false);
        if (estabaCargado) {
            actualizarCargado(archivo, equipo, jugador, cedulaAnterior);
        }
        jugadores.save(jugador);
        if (estabaCargado) {
            refrescarResultado(equipo, usuario);
        }
        return jugadorView(jugador);
    }

    private void actualizarCargado(RefuerzoArchivo archivo, RefuerzoEquipo equipo, RefuerzoJugador jugador, String cedulaAnterior) {
        Category categoria = categoria(archivo, equipo.getCategoria());
        if (categoria == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe la categoría en la edición seleccionada");
        }
        Team team = teams.findByLeague_IdAndNameAndEstadoNot(archivo.getIdLiga(), equipo.getNombreEquipo().trim(), "ELIMINADO")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no está inscrito en la liga"));
        TeamEntry entrada = entries.findByTeam_IdAndCategory_Id(team.getId(), categoria.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El equipo no está inscrito en la categoría"));
        List<Player> plantel = players.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId());
        Player player = plantel.stream()
                .filter(item -> item.getCedula().equalsIgnoreCase(cedulaAnterior) || item.getCedula().equalsIgnoreCase(jugador.getCedula()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador cargado ya no está en el equipo"));
        Integer numero = camisa(jugador.getNumeroCamisa());
        boolean cedulaRepetida = plantel.stream()
                .anyMatch(item -> !item.getId().equals(player.getId()) && item.getCedula().equalsIgnoreCase(jugador.getCedula()));
        boolean camisaRepetida = numero != null && plantel.stream()
                .anyMatch(item -> !item.getId().equals(player.getId())
                        && "ACTIVO".equals(item.getEstado()) && item.getNumeroCamisa() == numero);
        if (cedulaRepetida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada en el equipo");
        }
        if (camisaRepetida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El número de camisa ya está usado en el equipo");
        }
        player.setCedula(jugador.getCedula());
        player.setName(jugador.getNombre());
        player.setAlias(jugador.getAlias());
        player.setNumeroCamisa(numero);
        players.save(player);
        jugador.setMotivo(null);
        if (tieneFoto(jugador) || (jugador.getUrlFoto() != null && !jugador.getUrlFoto().isBlank() && !jugador.isFotoCargada())) {
            aplicarFoto(jugador, player);
        }
    }

    public FotoContenido foto(UUID jugadorId) {
        RefuerzoJugador jugador = jugadores.findById(jugadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado"));
        RefuerzoEquipo equipo = equipo(jugador.getIdRefuerzoEquipo());
        RefuerzoArchivo archivo = archivo(equipo.getIdRefuerzoArchivo());
        assertRead(archivo.getIdLiga());
        if (tieneFoto(jugador)) {
            return new FotoContenido(jugador.getFoto(), jugador.getTipoFoto() == null ? "image/jpeg" : jugador.getTipoFoto());
        }
        Player player = jugadorPlantel(archivo, equipo, jugador.getCedula());
        if (player == null || player.getPhoto() == null || player.getPhoto().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El jugador no tiene foto");
        }
        String tipo = player.getPhotoContentType() == null ? "image/jpeg" : player.getPhotoContentType();
        return new FotoContenido(player.getPhoto(), tipo);
    }

    private Player jugadorPlantel(RefuerzoArchivo archivo, RefuerzoEquipo equipo, String cedula) {
        Category categoria = categoria(archivo, equipo.getCategoria());
        if (categoria == null || equipo.getNombreEquipo() == null) {
            return null;
        }
        Team team = teams.findByLeague_IdAndNameAndEstadoNot(archivo.getIdLiga(), equipo.getNombreEquipo().trim(), "ELIMINADO").orElse(null);
        if (team == null) {
            return null;
        }
        TeamEntry entrada = entries.findByTeam_IdAndCategory_Id(team.getId(), categoria.getId()).orElse(null);
        if (entrada == null) {
            return null;
        }
        String buscada = cedula == null ? "" : cedula.trim();
        return players.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId()).stream()
                .filter(item -> item.getCedula().equalsIgnoreCase(buscada))
                .findFirst()
                .orElse(null);
    }

    private void refrescarResultado(RefuerzoEquipo equipo, String usuario) {
        List<RefuerzoJugador> plantel = jugadores.findByIdRefuerzoEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO");
        boolean alguno = plantel.stream().anyMatch(jugador -> jugador.isCargado() || jugador.isExistente());
        if (!alguno) {
            return;
        }
        cerrarPlantel(equipo, plantel, usuario);
    }

    private Category categoria(RefuerzoArchivo archivo, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return categorias.findByEdition_IdAndEstadoOrderByNameAsc(archivo.getIdEdicion(), "ACTIVO").stream()
                .filter(item -> item.getName() != null && item.getName().trim().equalsIgnoreCase(nombre.trim()))
                .findFirst()
                .orElse(null);
    }

    private long activos(List<Player> roster) {
        return roster.stream().filter(item -> "ACTIVO".equals(item.getEstado())).count();
    }

    private boolean camisaOcupada(List<Player> roster, int numero, UUID excepto) {
        return roster.stream().anyMatch(item -> "ACTIVO".equals(item.getEstado())
                && item.getNumeroCamisa() == numero
                && (excepto == null || !item.getId().equals(excepto)));
    }

    private boolean tieneFoto(RefuerzoJugador jugador) {
        return jugador.getFoto() != null && jugador.getFoto().length > 0;
    }

    private boolean esperaFoto(RefuerzoJugador jugador) {
        return (jugador.getUrlFoto() != null && !jugador.getUrlFoto().isBlank() || tieneFoto(jugador)) && !jugador.isFotoCargada();
    }

    private boolean visibleFoto(RefuerzoJugador jugador) {
        return tieneFoto(jugador) || jugador.isFotoCargada();
    }

    private void cerrar(RefuerzoEquipo equipo, String resultado, String detalle, String usuario) {
        equipo.setResultado(resultado);
        equipo.setDetalle(detalle);
        auditar(equipo, usuario, false);
        equipos.save(equipo);
    }

    private String etiqueta(RefuerzoJugador jugador) {
        String nombre = jugador.getNombre() == null || jugador.getNombre().isBlank() ? "Jugador " + jugador.getOrden() : jugador.getNombre().trim();
        String cedula = jugador.getCedula() == null || jugador.getCedula().isBlank() ? "sin cédula" : jugador.getCedula().trim();
        return nombre + " (" + cedula + "): ";
    }

    private String aliasDe(RefuerzoJugador jugador, String nombre) {
        if (jugador.getAlias() != null && !jugador.getAlias().isBlank()) {
            return jugador.getAlias();
        }
        return aliasDesdeNombre(nombre);
    }

    private Integer camisa(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        }
        if (!value.matches("\\d+")) {
            return null;
        }
        try {
            int number = Integer.parseInt(value);
            return number >= 0 ? number : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String aliasDesdeNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        String[] partes = limpio.split(" ");
        if (partes.length == 2) {
            return limpio;
        }
        if (partes.length >= 4) {
            return partes[0] + " " + partes[2];
        }
        return null;
    }

    private String limitar(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private void exigirEdicion(UUID leagueId, UUID editionId) {
        Edition edition = ediciones.findById(editionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Edición no encontrada"));
        if (!edition.getLeague().getId().equals(leagueId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La edición no pertenece a la liga seleccionada");
        }
    }

    private RefuerzoArchivo archivo(UUID id) {
        RefuerzoArchivo archivo = archivos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));
        if (!"ACTIVO".equals(archivo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        }
        return archivo;
    }

    private RefuerzoEquipo equipo(UUID id) {
        RefuerzoEquipo equipo = equipos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo de refuerzo no encontrado"));
        if (!"ACTIVO".equals(equipo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo de refuerzo no encontrado");
        }
        return equipo;
    }

    private List<RefuerzoEquipo> filas(UUID archivoId) {
        return equipos.findByIdRefuerzoArchivoAndEstadoOrderByNumeroFilaAsc(archivoId, "ACTIVO");
    }

    private RefuerzoDetalleView detalleView(RefuerzoArchivo archivo) {
        List<RefuerzoEquipoView> filas = filas(archivo.getId()).stream().map(this::equipoView).collect(Collectors.toList());
        return new RefuerzoDetalleView(archivo.getId(), archivo.getNombreArchivo(), filas);
    }

    private RefuerzoArchivoView archivoView(RefuerzoArchivo archivo) {
        return new RefuerzoArchivoView(archivo.getId(), archivo.getNombreArchivo(), archivo.getFechaCreacion().toString());
    }

    private RefuerzoEquipoView equipoView(RefuerzoEquipo equipo) {
        return new RefuerzoEquipoView(equipo.getId(), equipo.getNumeroFila(), equipo.getCategoria(), equipo.getNombreEquipo(),
                equipo.isSeleccionado(), equipo.getResultado(), equipo.getDetalle());
    }

    private RefuerzoJugadorView jugadorView(RefuerzoJugador jugador) {
        return new RefuerzoJugadorView(jugador.getId(), jugador.getCedula(), jugador.getNombre(), jugador.getAlias(),
                jugador.getNumeroCamisa(), jugador.getNumeroCamisaReemplazo(), jugador.isCargado(), jugador.isExistente(),
                jugador.isFotoCargada(), visibleFoto(jugador), jugador.getMotivo());
    }

    private void auditar(RefuerzoArchivo row, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            row.setFechaCreacion(ahora);
            row.setUsuarioCreacion(usuario);
            row.setEstado("ACTIVO");
        }
        row.setFechaModificacion(ahora);
        row.setUsuarioModificacion(usuario);
    }

    private void auditar(RefuerzoEquipo row, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            row.setFechaCreacion(ahora);
            row.setUsuarioCreacion(usuario);
            row.setEstado("ACTIVO");
        }
        row.setFechaModificacion(ahora);
        row.setUsuarioModificacion(usuario);
    }

    private void auditar(RefuerzoJugador row, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            row.setFechaCreacion(ahora);
            row.setUsuarioCreacion(usuario);
            row.setEstado("ACTIVO");
        }
        row.setFechaModificacion(ahora);
        row.setUsuarioModificacion(usuario);
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

    static final class FotoContenido {
        final byte[] bytes;
        final String tipo;

        private FotoContenido(byte[] bytes, String tipo) {
            this.bytes = bytes;
            this.tipo = tipo;
        }
    }
}
