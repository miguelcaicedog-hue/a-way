package com.away.core.competition;

import com.away.core.auth.AppUser;
import com.away.core.auth.AppUserRepository;
import com.away.core.auth.League;
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
public class InscripcionService {

    private final InscripcionArchivoRepository archivos;
    private final InscripcionEquipoRepository equipos;
    private final InscripcionJugadorRepository jugadores;
    private final EditionRepository ediciones;
    private final CategoryRepository categorias;
    private final TeamRepository teams;
    private final TeamEntryRepository entries;
    private final PlayerRepository players;
    private final AppUserRepository users;
    private final TransactionTemplate transactions;

    public InscripcionService(InscripcionArchivoRepository archivos,
                              InscripcionEquipoRepository equipos,
                              InscripcionJugadorRepository jugadores,
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
    public List<InscripcionArchivoView> listar(UUID leagueId, UUID editionId) {
        assertRead(leagueId);
        exigirEdicion(leagueId, editionId);
        return archivos.findByIdLigaAndIdEdicionAndEstadoOrderByFechaCreacionDesc(leagueId, editionId, "ACTIVO").stream()
                .map(this::archivoView)
                .collect(Collectors.toList());
    }

    @Transactional
    public void eliminar(UUID archivoId) {
        InscripcionArchivo archivo = archivo(archivoId);
        assertWrite(archivo.getIdLiga());
        if (!"ACTIVO".equals(archivo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo ya no está activo");
        }
        List<InscripcionEquipo> cargados = equipos.findByIdInscripcionArchivoAndEstadoOrderByNumeroFilaAsc(archivo.getId(), "ACTIVO");
        boolean procesado = cargados.stream().anyMatch(equipo -> "OK".equals(equipo.getResultado()));
        if (procesado) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo ya fue procesado");
        }
        String usuario = usuario();
        for (InscripcionEquipo equipo : cargados) {
            for (InscripcionJugador jugador : jugadores.findByIdInscripcionEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO")) {
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
    public InscripcionDetalleView detalle(UUID archivoId) {
        InscripcionArchivo archivo = archivo(archivoId);
        assertRead(archivo.getIdLiga());
        return detalleView(archivo);
    }

    @Transactional(readOnly = true)
    public List<InscripcionJugadorView> jugadores(UUID equipoId) {
        InscripcionEquipo equipo = equipo(equipoId);
        InscripcionArchivo archivo = archivo(equipo.getIdInscripcionArchivo());
        assertRead(archivo.getIdLiga());
        return jugadores.findByIdInscripcionEquipoAndEstadoOrderByOrdenAsc(equipoId, "ACTIVO").stream()
                .map(this::jugadorView)
                .collect(Collectors.toList());
    }

    @Transactional
    public InscripcionDetalleView cargar(UUID leagueId, UUID editionId, MultipartFile archivo) {
        assertWrite(leagueId);
        exigirEdicion(leagueId, editionId);
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un archivo de Excel");
        }
        String nombre = archivo.getOriginalFilename() == null ? "inscripcion.xlsx" : archivo.getOriginalFilename();
        if (!nombre.toLowerCase().endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe ser .xlsx");
        }
        List<InscripcionWorkbook.EquipoLeido> leidos;
        try {
            leidos = InscripcionWorkbook.leer(archivo.getInputStream());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo de Excel");
        }
        if (leidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no tiene equipos");
        }
        String usuario = usuario();
        InscripcionArchivo guardado = new InscripcionArchivo();
        guardado.setId(UUID.randomUUID());
        guardado.setIdLiga(leagueId);
        guardado.setIdEdicion(editionId);
        guardado.setNombreArchivo(limitar(nombre, 255));
        auditar(guardado, usuario, true);
        archivos.save(guardado);
        for (InscripcionWorkbook.EquipoLeido leido : leidos) {
            InscripcionEquipo equipo = new InscripcionEquipo();
            equipo.setId(UUID.randomUUID());
            equipo.setIdInscripcionArchivo(guardado.getId());
            equipo.setNumeroFila(leido.fila);
            equipo.setCategoria(leido.categoria == null ? "" : leido.categoria);
            equipo.setNombreEquipo(leido.nombre);
            equipo.setUrlLogo(leido.logo);
            equipo.setNombreRepresentante(leido.representante);
            equipo.setNumeroTelefono(leido.telefono);
            equipo.setSeleccionado(true);
            auditar(equipo, usuario, true);
            equipos.save(equipo);
            for (InscripcionWorkbook.JugadorLeido jugadorLeido : leido.jugadores) {
                InscripcionJugador jugador = new InscripcionJugador();
                jugador.setId(UUID.randomUUID());
                jugador.setIdInscripcionEquipo(equipo.getId());
                jugador.setOrden(jugadorLeido.orden);
                jugador.setCedula(jugadorLeido.cedula);
                jugador.setNombre(jugadorLeido.nombre);
                jugador.setNumeroCamisa(jugadorLeido.numeroCamisa);
                jugador.setUrlFoto(jugadorLeido.urlFoto);
                jugador.setCargado(false);
                jugador.setFotoCargada(false);
                auditar(jugador, usuario, true);
                jugadores.save(jugador);
            }
        }
        return detalleView(guardado);
    }

    @Transactional
    public InscripcionEquipoView seleccionar(UUID equipoId, boolean seleccionado) {
        InscripcionEquipo equipo = equipo(equipoId);
        InscripcionArchivo archivo = archivo(equipo.getIdInscripcionArchivo());
        assertWrite(archivo.getIdLiga());
        equipo.setSeleccionado(seleccionado);
        auditar(equipo, usuario(), false);
        return equipoView(equipos.save(equipo));
    }

    public InscripcionDetalleView procesar(UUID archivoId, List<UUID> equipoIds) {
        InscripcionArchivo archivo = archivo(archivoId);
        assertWrite(archivo.getIdLiga());
        Set<UUID> elegidos = new HashSet<>(equipoIds == null ? List.of() : equipoIds);
        String usuario = usuario();
        transactions.executeWithoutResult(status -> {
            for (InscripcionEquipo equipo : filas(archivoId)) {
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
        InscripcionArchivo archivo = archivo(archivoId);
        assertWrite(archivo.getIdLiga());
        Set<UUID> elegidos = new HashSet<>(equipoIds == null ? List.of() : equipoIds);
        List<String> muestras = new ArrayList<>();
        for (InscripcionEquipo equipo : filas(archivoId)) {
            if (!elegidos.contains(equipo.getId()) || "OK".equals(equipo.getResultado())) {
                continue;
            }
            for (InscripcionJugador jugador : jugadores.findByIdInscripcionEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO")) {
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

    public InscripcionEquipoView procesarUno(UUID equipoId) {
        InscripcionEquipo equipo = equipo(equipoId);
        InscripcionArchivo archivo = archivo(equipo.getIdInscripcionArchivo());
        assertWrite(archivo.getIdLiga());
        String usuario = usuario();
        transactions.executeWithoutResult(status -> procesarEquipo(archivo.getId(), equipoId, usuario));
        return equipoView(equipos.findById(equipoId).orElseThrow());
    }

    private void procesarEquipo(UUID archivoId, UUID equipoId, String usuario) {
        InscripcionEquipo equipo = equipos.findById(equipoId).orElse(null);
        if (equipo == null || !archivoId.equals(equipo.getIdInscripcionArchivo()) || "OK".equals(equipo.getResultado())) {
            return;
        }
        InscripcionArchivo archivo = archivo(archivoId);
        List<InscripcionJugador> plantel = jugadores.findByIdInscripcionEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO");
        if (equipo.getCategoria() == null || equipo.getCategoria().isBlank()) {
            cerrar(equipo, "SIN CARGA", "La fila no tiene categoría.", usuario);
            return;
        }
        if (plantel.isEmpty()) {
            cerrar(equipo, "SIN CARGA", "El equipo no tiene jugadores para cargar.", usuario);
            return;
        }
        Category categoria = categorias.findByEdition_IdAndEstadoOrderByNameAsc(archivo.getIdEdicion(), "ACTIVO").stream()
                .filter(item -> item.getName() != null && item.getName().trim().equalsIgnoreCase(equipo.getCategoria().trim()))
                .findFirst()
                .orElse(null);
        if (categoria == null) {
            cerrar(equipo, "SIN CARGA", "No existe la categoría " + equipo.getCategoria().trim() + " en la edición seleccionada.", usuario);
            return;
        }
        if (equipo.getNombreEquipo() == null || equipo.getNombreEquipo().isBlank() || equipo.getNombreEquipo().length() > 160) {
            cerrar(equipo, "SIN CARGA", "El nombre del equipo no se puede guardar.", usuario);
            return;
        }
        Team team = teams.findByLeague_IdAndNameAndEstadoNot(archivo.getIdLiga(), equipo.getNombreEquipo().trim(), "ELIMINADO")
                .orElseGet(() -> nuevoEquipo(ediciones.findById(archivo.getIdEdicion()).orElseThrow().getLeague(), equipo.getNombreEquipo().trim(), null, null));
        List<String> avisos = new ArrayList<>();
        String telefono = telefono(equipo.getNumeroTelefono());
        if (equipo.getNumeroTelefono() != null && telefono != null && normalizar(equipo.getNumeroTelefono()).length() > 30) {
            avisos.add("El teléfono supera 30 caracteres y se guardó recortado.");
        }
        team.setNombreRepresentante(equipo.getNombreRepresentante());
        team.setNumeroTelefono(telefono);
        String avisoLogo = aplicarLogoEquipo(team, equipo.getUrlLogo());
        auditarEquipo(team, false);
        teams.save(team);
        TeamEntry entry = entries.findByTeam_IdAndCategory_Id(team.getId(), categoria.getId()).orElseGet(() -> {
            TeamEntry nuevo = new TeamEntry();
            nuevo.setId(UUID.randomUUID());
            nuevo.setTeam(team);
            nuevo.setCategory(categoria);
            return entries.save(nuevo);
        });
        List<Player> existentes = new ArrayList<>(players.findByTeamEntry_IdOrderByCedulaAsc(entry.getId()));
        Set<String> cedulas = existentes.stream().map(Player::getCedula).collect(Collectors.toSet());
        Set<Integer> camisas = existentes.stream().map(Player::getNumeroCamisa).collect(Collectors.toSet());
        int porCargar = 0;
        for (InscripcionJugador jugador : plantel) {
            if (!jugador.isCargado() && camisaValida(jugador) && cedulaValida(jugador)
                    && !cedulas.contains(jugador.getCedula().trim()) && !camisas.contains(camisa(jugador.getNumeroCamisa()))) {
                porCargar++;
            }
        }
        int ocupados = existentes.size() + porCargar;
        if (ocupados > categoria.getMaxPlayers()) {
            categoria.setMaxPlayers(ocupados);
            categorias.save(categoria);
        }
        for (InscripcionJugador jugador : plantel) {
            auditar(jugador, usuario, false);
            if (jugador.isCargado()) {
                Player guardado = existentes.stream()
                        .filter(item -> item.getCedula().equals(jugador.getCedula().trim()))
                        .findFirst()
                        .orElse(null);
                if (guardado != null) {
                    if (guardado.getAlias() == null || guardado.getAlias().isBlank()) {
                        String alias = aliasDesdeNombre(guardado.getName());
                        if (alias != null) {
                            guardado.setAlias(alias);
                            jugador.setAlias(alias);
                            players.save(guardado);
                        }
                    }
                    if (!jugador.isFotoCargada() && esperaFoto(jugador)) {
                        aplicarFoto(jugador, guardado);
                    }
                }
                continue;
            }
            String cedula = jugador.getCedula() == null ? "" : jugador.getCedula().trim();
            String nombre = jugador.getNombre() == null ? "" : jugador.getNombre().trim();
            Integer numero = camisa(jugador.getNumeroCamisa());
            if (cedula.isEmpty() || cedula.length() > 30) {
                jugador.setMotivo(etiqueta(jugador) + "revisar la cédula.");
                continue;
            }
            if (nombre.isEmpty()) {
                jugador.setMotivo(etiqueta(jugador) + "falta el nombre.");
                continue;
            }
            if (numero == null) {
                jugador.setMotivo(etiqueta(jugador) + "el número de camisa no es válido.");
                continue;
            }
            if (cedulas.contains(cedula)) {
                jugador.setMotivo(etiqueta(jugador) + "la cédula ya está registrada en el equipo.");
                continue;
            }
            if (camisas.contains(numero)) {
                jugador.setMotivo(etiqueta(jugador) + "el número de camisa ya está usado en el equipo.");
                continue;
            }
            Player player = new Player();
            player.setId(UUID.randomUUID());
            player.setTeamEntry(entry);
            player.setName(nombre);
            player.setCedula(cedula);
            player.setNumeroCamisa(numero);
            player.setAlias(aliasDesdeNombre(nombre));
            player.setRotationDegrees((short) 0);
            players.save(player);
            jugador.setCargado(true);
            jugador.setAlias(player.getAlias());
            jugador.setMotivo(null);
            cedulas.add(cedula);
            camisas.add(numero);
            existentes.add(player);
            if (esperaFoto(jugador)) {
                aplicarFoto(jugador, player);
            }
        }
        boolean alguno = plantel.stream().anyMatch(InscripcionJugador::isCargado);
        boolean pendiente = plantel.stream().anyMatch(jugador -> !jugador.isCargado() || esperaFoto(jugador));
        for (InscripcionJugador jugador : plantel) {
            if (jugador.getMotivo() != null && !jugador.getMotivo().isBlank()) {
                avisos.add(jugador.getMotivo());
            }
        }
        if (!alguno && avisos.stream().noneMatch(aviso -> aviso.startsWith("El teléfono"))) {
            avisos.add(0, "No se cargó ningún jugador.");
        }
        String detalle = avisos.stream().filter(aviso -> aviso != null && !aviso.isBlank()).collect(Collectors.joining("\n"));
        if (!pendiente && detalle.isBlank()) {
            cerrar(equipo, "OK", avisoLogo, usuario);
        } else {
            if (avisoLogo != null) {
                detalle = detalle.isBlank() ? avisoLogo : avisoLogo + "\n" + detalle;
            }
            cerrar(equipo, "ERROR", detalle, usuario);
        }
        jugadores.saveAll(plantel);
    }

    private String aplicarLogoEquipo(Team team, String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        FotoInscripcion.Descarga descarga = FotoInscripcion.descargar(url);
        if (descarga.error != null || descarga.bytes == null || descarga.bytes.length == 0 || descarga.bytes.length > 2_000_000) {
            return "No se pudo cargar el logo del equipo.";
        }
        team.setLogo(descarga.bytes);
        team.setLogoContentType(descarga.contentType == null ? "image/jpeg" : descarga.contentType);
        return null;
    }

    private void aplicarFoto(InscripcionJugador jugador, Player player) {
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
    public InscripcionJugadorView editarJugador(UUID jugadorId, String cedula, String nombre, String alias,
                                                String numeroCamisa, MultipartFile foto) {
        InscripcionJugador jugador = jugadores.findById(jugadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado"));
        if (!"ACTIVO".equals(jugador.getEstado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado");
        }
        InscripcionEquipo equipo = equipo(jugador.getIdInscripcionEquipo());
        InscripcionArchivo archivo = archivo(equipo.getIdInscripcionArchivo());
        assertWrite(archivo.getIdLiga());
        String cedulaNueva = cedula == null ? "" : cedula.trim();
        String nombreNuevo = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        String camisaNueva = numeroCamisa == null ? "" : numeroCamisa.trim();
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
        jugador.setCedula(cedulaNueva);
        jugador.setNombre(limitar(nombreNuevo, 200));
        jugador.setNumeroCamisa(String.valueOf(numero));
        jugador.setAlias(aliasNuevo);
        if (fotoNueva != null) {
            jugador.setFoto(fotoNueva);
            jugador.setTipoFoto(tipoFoto);
        }
        String usuario = usuario();
        auditar(jugador, usuario, false);
        incorporar(archivo, equipo, jugador, cedulaAnterior);
        jugadores.save(jugador);
        refrescarResultado(equipo, usuario);
        return jugadorView(jugador);
    }

    public FotoContenido foto(UUID jugadorId) {
        InscripcionJugador jugador = jugadores.findById(jugadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jugador no encontrado"));
        InscripcionEquipo equipo = equipo(jugador.getIdInscripcionEquipo());
        InscripcionArchivo archivo = archivo(equipo.getIdInscripcionArchivo());
        assertRead(archivo.getIdLiga());
        if (tieneFoto(jugador)) {
            return new FotoContenido(jugador.getFoto(), jugador.getTipoFoto() == null ? "image/jpeg" : jugador.getTipoFoto());
        }
        Player player = jugadorPlantel(archivo, equipo, jugador.getCedula(), jugador.getCedula());
        if (player == null || player.getPhoto() == null || player.getPhoto().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El jugador no tiene foto");
        }
        String tipo = player.getPhotoContentType() == null ? "image/jpeg" : player.getPhotoContentType();
        return new FotoContenido(player.getPhoto(), tipo);
    }

    private void incorporar(InscripcionArchivo archivo, InscripcionEquipo equipo, InscripcionJugador jugador, String cedulaAnterior) {
        if (equipo.getCategoria() == null || equipo.getCategoria().isBlank()) {
            jugador.setMotivo(etiqueta(jugador) + "la fila no tiene categoría.");
            return;
        }
        Category categoria = categorias.findByEdition_IdAndEstadoOrderByNameAsc(archivo.getIdEdicion(), "ACTIVO").stream()
                .filter(item -> item.getName() != null && item.getName().trim().equalsIgnoreCase(equipo.getCategoria().trim()))
                .findFirst()
                .orElse(null);
        if (categoria == null) {
            jugador.setMotivo(etiqueta(jugador) + "no existe la categoría " + equipo.getCategoria().trim() + " en la edición seleccionada.");
            return;
        }
        if (equipo.getNombreEquipo() == null || equipo.getNombreEquipo().isBlank()) {
            jugador.setMotivo(etiqueta(jugador) + "el equipo no se puede guardar.");
            return;
        }
        Team team = teams.findByLeague_IdAndNameAndEstadoNot(archivo.getIdLiga(), equipo.getNombreEquipo().trim(), "ELIMINADO")
                .orElseGet(() -> nuevoEquipo(
                        ediciones.findById(archivo.getIdEdicion()).orElseThrow().getLeague(),
                        equipo.getNombreEquipo().trim(),
                        equipo.getNombreRepresentante(),
                        telefono(equipo.getNumeroTelefono())));
        TeamEntry entrada = entries.findByTeam_IdAndCategory_Id(team.getId(), categoria.getId()).orElseGet(() -> {
            TeamEntry nuevo = new TeamEntry();
            nuevo.setId(UUID.randomUUID());
            nuevo.setTeam(team);
            nuevo.setCategory(categoria);
            return entries.save(nuevo);
        });
        List<Player> plantel = new ArrayList<>(players.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId()));
        Player existente = plantel.stream()
                .filter(item -> item.getCedula().equalsIgnoreCase(cedulaAnterior) || item.getCedula().equalsIgnoreCase(jugador.getCedula()))
                .findFirst()
                .orElse(null);
        UUID existenteId = existente == null ? null : existente.getId();
        boolean cedulaRepetida = plantel.stream()
                .anyMatch(item -> (existenteId == null || !item.getId().equals(existenteId)) && item.getCedula().equalsIgnoreCase(jugador.getCedula()));
        Integer numero = camisa(jugador.getNumeroCamisa());
        boolean camisaRepetida = numero != null && plantel.stream()
                .anyMatch(item -> (existenteId == null || !item.getId().equals(existenteId)) && item.getNumeroCamisa() == numero);
        if (cedulaRepetida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cédula ya está registrada en el equipo");
        }
        if (camisaRepetida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El número de camisa ya está usado en el equipo");
        }
        if (existente == null && plantel.size() + 1 > categoria.getMaxPlayers()) {
            categoria.setMaxPlayers(plantel.size() + 1);
            categorias.save(categoria);
        }
        Player player = existente;
        if (player == null) {
            player = new Player();
            player.setId(UUID.randomUUID());
            player.setTeamEntry(entrada);
            player.setRotationDegrees((short) 0);
        }
        player.setCedula(jugador.getCedula());
        player.setName(jugador.getNombre());
        player.setAlias(jugador.getAlias());
        player.setNumeroCamisa(numero);
        players.save(player);
        jugador.setCargado(true);
        jugador.setMotivo(null);
        if (tieneFoto(jugador) || (jugador.getUrlFoto() != null && !jugador.getUrlFoto().isBlank() && !jugador.isFotoCargada())) {
            aplicarFoto(jugador, player);
        }
    }

    private boolean visibleFoto(InscripcionJugador jugador) {
        if (tieneFoto(jugador) || jugador.isFotoCargada()) {
            return true;
        }
        return false;
    }

    private Player jugadorPlantel(InscripcionArchivo archivo, InscripcionEquipo equipo, String cedula, String cedulaAlterna) {
        TeamEntry entrada = entrada(archivo, equipo);
        if (entrada == null) {
            return null;
        }
        return players.findByTeamEntry_IdOrderByCedulaAsc(entrada.getId()).stream()
                .filter(item -> item.getCedula().equalsIgnoreCase(cedula == null ? "" : cedula.trim())
                        || item.getCedula().equalsIgnoreCase(cedulaAlterna == null ? "" : cedulaAlterna.trim()))
                .findFirst()
                .orElse(null);
    }

    private boolean tieneFoto(InscripcionJugador jugador) {
        return jugador.getFoto() != null && jugador.getFoto().length > 0;
    }

    private boolean esperaFoto(InscripcionJugador jugador) {
        return (jugador.getUrlFoto() != null || tieneFoto(jugador)) && !jugador.isFotoCargada();
    }

    private TeamEntry entrada(InscripcionArchivo archivo, InscripcionEquipo equipo) {
        if (equipo.getCategoria() == null || equipo.getCategoria().isBlank() || equipo.getNombreEquipo() == null) {
            return null;
        }
        Category categoria = categorias.findByEdition_IdAndEstadoOrderByNameAsc(archivo.getIdEdicion(), "ACTIVO").stream()
                .filter(item -> item.getName() != null && item.getName().trim().equalsIgnoreCase(equipo.getCategoria().trim()))
                .findFirst()
                .orElse(null);
        if (categoria == null) {
            return null;
        }
        Team team = teams.findByLeague_IdAndNameAndEstadoNot(archivo.getIdLiga(), equipo.getNombreEquipo().trim(), "ELIMINADO").orElse(null);
        if (team == null) {
            return null;
        }
        return entries.findByTeam_IdAndCategory_Id(team.getId(), categoria.getId()).orElse(null);
    }

    private void refrescarResultado(InscripcionEquipo equipo, String usuario) {
        List<InscripcionJugador> plantel = jugadores.findByIdInscripcionEquipoAndEstadoOrderByOrdenAsc(equipo.getId(), "ACTIVO");
        boolean alguno = plantel.stream().anyMatch(InscripcionJugador::isCargado);
        if (!alguno) {
            return;
        }
        List<String> avisos = new ArrayList<>();
        for (InscripcionJugador jugador : plantel) {
            if (jugador.getMotivo() != null && !jugador.getMotivo().isBlank()) {
                avisos.add(jugador.getMotivo());
            }
        }
        boolean pendiente = plantel.stream().anyMatch(jugador -> !jugador.isCargado() || esperaFoto(jugador));
        String detalle = avisos.stream().filter(aviso -> aviso != null && !aviso.isBlank()).collect(Collectors.joining("\n"));
        if (!pendiente && detalle.isBlank()) {
            cerrar(equipo, "OK", null, usuario);
        } else {
            cerrar(equipo, "ERROR", detalle.isBlank() ? null : detalle, usuario);
        }
    }

    private boolean cedulaValida(InscripcionJugador jugador) {
        String cedula = jugador.getCedula() == null ? "" : jugador.getCedula().trim();
        return !cedula.isEmpty() && cedula.length() <= 30 && jugador.getNombre() != null && !jugador.getNombre().isBlank();
    }

    private boolean camisaValida(InscripcionJugador jugador) {
        return camisa(jugador.getNumeroCamisa()) != null;
    }

    private void cerrar(InscripcionEquipo equipo, String resultado, String detalle, String usuario) {
        equipo.setResultado(resultado);
        equipo.setDetalle(detalle);
        auditar(equipo, usuario, false);
        equipos.save(equipo);
    }

    private String etiqueta(InscripcionJugador jugador) {
        String nombre = jugador.getNombre() == null || jugador.getNombre().isBlank() ? "Jugador " + jugador.getOrden() : jugador.getNombre().trim();
        String cedula = jugador.getCedula() == null || jugador.getCedula().isBlank() ? "sin cédula" : jugador.getCedula().trim();
        return nombre + " (" + cedula + "): ";
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

    private String telefono(String raw) {
        String value = normalizar(raw);
        if (value.isEmpty()) {
            return null;
        }
        return value.length() <= 30 ? value : value.substring(0, 30);
    }

    private String normalizar(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace('\u202A', ' ')
                .replace('\u202C', ' ')
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .trim()
                .replaceAll("\\s+", " ");
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

    private InscripcionArchivo archivo(UUID id) {
        InscripcionArchivo archivo = archivos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado"));
        if (!"ACTIVO".equals(archivo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        }
        return archivo;
    }

    private InscripcionEquipo equipo(UUID id) {
        InscripcionEquipo equipo = equipos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo de inscripción no encontrado"));
        if (!"ACTIVO".equals(equipo.getEstado())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo de inscripción no encontrado");
        }
        return equipo;
    }

    private List<InscripcionEquipo> filas(UUID archivoId) {
        return equipos.findByIdInscripcionArchivoAndEstadoOrderByNumeroFilaAsc(archivoId, "ACTIVO");
    }

    private InscripcionDetalleView detalleView(InscripcionArchivo archivo) {
        List<InscripcionEquipoView> filas = filas(archivo.getId()).stream().map(this::equipoView).collect(Collectors.toList());
        return new InscripcionDetalleView(archivo.getId(), archivo.getNombreArchivo(), filas);
    }

    private InscripcionArchivoView archivoView(InscripcionArchivo archivo) {
        return new InscripcionArchivoView(archivo.getId(), archivo.getNombreArchivo(), archivo.getFechaCreacion().toString());
    }

    private InscripcionEquipoView equipoView(InscripcionEquipo equipo) {
        return new InscripcionEquipoView(equipo.getId(), equipo.getNumeroFila(), equipo.getCategoria(), equipo.getNombreEquipo(),
                equipo.getUrlLogo(), equipo.getNombreRepresentante(), equipo.getNumeroTelefono(), equipo.isSeleccionado(), equipo.getResultado(), equipo.getDetalle());
    }

    private InscripcionJugadorView jugadorView(InscripcionJugador jugador) {
        return new InscripcionJugadorView(jugador.getId(), jugador.getCedula(), jugador.getNombre(), jugador.getAlias(),
                jugador.getNumeroCamisa(), jugador.isCargado(), jugador.isFotoCargada(), visibleFoto(jugador), jugador.getMotivo());
    }

    private Team nuevoEquipo(League league, String nombre, String representante, String telefono) {
        Team nuevo = new Team();
        nuevo.setId(UUID.randomUUID());
        nuevo.setLeague(league);
        nuevo.setName(nombre);
        nuevo.setNombreRepresentante(representante);
        nuevo.setNumeroTelefono(telefono);
        auditarEquipo(nuevo, true);
        return teams.save(nuevo);
    }

    private void auditarEquipo(Team team, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            team.setFechaCreacion(ahora);
            team.setUsuarioCreacion(usuario());
            team.setEstado("ACTIVO");
        }
        team.setFechaModificacion(ahora);
        team.setUsuarioModificacion(usuario());
    }

    private void auditar(InscripcionArchivo row, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            row.setFechaCreacion(ahora);
            row.setUsuarioCreacion(usuario);
            row.setEstado("ACTIVO");
        }
        row.setFechaModificacion(ahora);
        row.setUsuarioModificacion(usuario);
    }

    private void auditar(InscripcionEquipo row, String usuario, boolean nuevo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (nuevo) {
            row.setFechaCreacion(ahora);
            row.setUsuarioCreacion(usuario);
            row.setEstado("ACTIVO");
        }
        row.setFechaModificacion(ahora);
        row.setUsuarioModificacion(usuario);
    }

    private void auditar(InscripcionJugador row, String usuario, boolean nuevo) {
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
