package com.away.core.competition;

import com.away.core.auth.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LeagueRepository extends JpaRepository<League, UUID> {
    List<League> findAllByOrderByNameAsc();
}

interface LeagueSettingsRepository extends JpaRepository<LeagueSettings, UUID> {
}

interface FormatoPublicacionRepository extends JpaRepository<FormatoPublicacion, UUID> {
    Optional<FormatoPublicacion> findByIdLigaAndEstado(UUID idLiga, String estado);

    void deleteByIdLiga(UUID idLiga);
}

interface FormatoPublicacionTablaRepository extends JpaRepository<FormatoPublicacionTabla, UUID> {
    List<FormatoPublicacionTabla> findByIdLigaAndEstado(UUID idLiga, String estado);

    void deleteByIdLiga(UUID idLiga);
}

interface ReglaSuspensionRepository extends JpaRepository<ReglaSuspension, UUID> {
    Optional<ReglaSuspension> findByIdLigaAndEstado(UUID idLiga, String estado);
}

interface EditionRepository extends JpaRepository<Edition, UUID> {
    List<Edition> findByLeague_IdOrderByStartsOnDesc(UUID leagueId);
}

interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByEdition_IdOrderByNameAsc(UUID editionId);

    List<Category> findByEdition_IdAndEstadoOrderByNameAsc(UUID editionId, String estado);
}

interface TeamRepository extends JpaRepository<Team, UUID> {
    @Query("select team from Team team "
            + "where team.league.id = :leagueId "
            + "and (:estado = '' or team.estado = :estado) "
            + "and (:nombre = '' or lower(team.name) like lower(concat('%', :nombre, '%'))) "
            + "and (:representante = '' or lower(coalesce(team.nombreRepresentante, '')) like lower(concat('%', :representante, '%'))) "
            + "order by team.name asc")
    List<Team> buscar(@Param("leagueId") UUID leagueId,
                      @Param("nombre") String nombre,
                      @Param("representante") String representante,
                      @Param("estado") String estado);

    Optional<Team> findByLeague_IdAndNameAndEstadoNot(UUID leagueId, String name, String estado);
}

interface InscripcionArchivoRepository extends JpaRepository<InscripcionArchivo, UUID> {
    List<InscripcionArchivo> findByIdLigaAndIdEdicionAndEstadoOrderByFechaCreacionDesc(UUID idLiga, UUID idEdicion, String estado);
}

interface InscripcionEquipoRepository extends JpaRepository<InscripcionEquipo, UUID> {
    List<InscripcionEquipo> findByIdInscripcionArchivoAndEstadoOrderByNumeroFilaAsc(UUID idInscripcionArchivo, String estado);
}

interface InscripcionJugadorRepository extends JpaRepository<InscripcionJugador, UUID> {
    List<InscripcionJugador> findByIdInscripcionEquipoAndEstadoOrderByOrdenAsc(UUID idInscripcionEquipo, String estado);
}

interface TeamEntryRepository extends JpaRepository<TeamEntry, UUID> {
    List<TeamEntry> findByCategory_IdOrderByTeam_NameAsc(UUID categoryId);

    Optional<TeamEntry> findByTeam_IdAndCategory_Id(UUID teamId, UUID categoryId);

    @Query("select entry from TeamEntry entry join fetch entry.category category "
            + "where entry.team.id in :teamIds and category.edition.id = :editionId")
    List<TeamEntry> findByEquiposEnEdicion(@Param("teamIds") Collection<UUID> teamIds, @Param("editionId") UUID editionId);

    @Query("select entry from TeamEntry entry join fetch entry.category category "
            + "where entry.team.id = :teamId and category.edition.id = :editionId")
    List<TeamEntry> findByEquipoEnEdicion(@Param("teamId") UUID teamId, @Param("editionId") UUID editionId);
}

interface PlayerRepository extends JpaRepository<Player, UUID> {
    List<Player> findByTeamEntry_IdOrderByCedulaAsc(UUID teamEntryId);

    List<Player> findByCedulaIgnoreCaseOrderByNameAsc(String cedula);

    @Query("select player.teamEntry.team.id, count(player.id) from Player player "
            + "where player.teamEntry.category.edition.id = :editionId and player.teamEntry.team.id in :teamIds "
            + "and player.estado = 'ACTIVO' "
            + "group by player.teamEntry.team.id")
    List<Object[]> contarPorEquipo(@Param("editionId") UUID editionId, @Param("teamIds") Collection<UUID> teamIds);
}

interface GrupoRepository extends JpaRepository<Grupo, UUID> {
    List<Grupo> findByIdCategoriaAndEstadoOrderByOrdenAsc(UUID idCategoria, String estado);
}

interface GrupoEquipoRepository extends JpaRepository<GrupoEquipo, UUID> {
    List<GrupoEquipo> findByIdCategoriaAndEstadoOrderByOrdenAsc(UUID idCategoria, String estado);

    List<GrupoEquipo> findByIdGrupoAndEstado(UUID idGrupo, String estado);
}

interface FaseRepository extends JpaRepository<Fase, UUID> {
    List<Fase> findByIdCategoriaAndEstadoOrderByOrdenAsc(UUID idCategoria, String estado);
}

interface CruceRepository extends JpaRepository<Cruce, UUID> {
    List<Cruce> findByIdCategoriaAndEstadoOrderByOrdenAsc(UUID idCategoria, String estado);

    List<Cruce> findByIdFaseAndEstadoOrderByOrdenAsc(UUID idFase, String estado);
}

interface JornadaRepository extends JpaRepository<Jornada, UUID> {
    List<Jornada> findByIdCategoriaAndEstadoOrderByNumeroAsc(UUID idCategoria, String estado);

    List<Jornada> findByIdCategoriaInAndEstado(Collection<UUID> categorias, String estado);
}

interface SuspensionJugadorRepository extends JpaRepository<SuspensionJugador, UUID> {
    List<SuspensionJugador> findByIdCategoriaAndEstado(UUID idCategoria, String estado);

    List<SuspensionJugador> findByIdCategoriaAndIdJugadorAndEstado(UUID idCategoria, UUID idJugador, String estado);
}

interface SuspensionExclusionRepository extends JpaRepository<SuspensionExclusion, UUID> {
    List<SuspensionExclusion> findByIdCategoriaAndEstado(UUID idCategoria, String estado);

    Optional<SuspensionExclusion> findByIdJugadorAndIdJornadaOrigenAndMotivoAndEstado(
            UUID idJugador, UUID idJornadaOrigen, String motivo, String estado);
}

interface PartidoRepository extends JpaRepository<Partido, UUID> {
    List<Partido> findByIdCategoriaAndEstadoOrderByOrdenAsc(UUID idCategoria, String estado);

    List<Partido> findByIdJornadaAndEstadoOrderByOrdenAsc(UUID idJornada, String estado);

    List<Partido> findByIdCategoriaInAndEstado(Collection<UUID> categorias, String estado);
}

interface ArbitroRepository extends JpaRepository<Arbitro, UUID> {
    @Query("select a from Arbitro a where a.idLiga = :liga "
            + "and (:estado = '' "
            + "or (:estado = 'INACTIVO' and a.estado in ('INACTIVO', 'ELIMINADO')) "
            + "or (:estado <> 'INACTIVO' and a.estado = :estado)) "
            + "and (:cedula = '' or lower(coalesce(a.cedula, '')) like lower(concat('%', :cedula, '%'))) "
            + "and (:nombre = '' or lower(a.nombre) like lower(concat('%', :nombre, '%'))) "
            + "order by a.nombre")
    List<Arbitro> buscar(@Param("liga") UUID liga,
                         @Param("cedula") String cedula,
                         @Param("nombre") String nombre,
                         @Param("estado") String estado);
}

interface SancionJugadorRepository extends JpaRepository<SancionJugador, UUID> {
    @Query("select s from SancionJugador s where s.idLiga = :liga "
            + "and (:estado = '' or s.estado = :estado) "
            + "and (:descripcion = '' or lower(s.descripcion) like lower(concat('%', :descripcion, '%'))) "
            + "order by s.descripcion")
    List<SancionJugador> buscar(@Param("liga") UUID liga,
                                @Param("descripcion") String descripcion,
                                @Param("estado") String estado);
}

interface TarjetaRepository extends JpaRepository<Tarjeta, UUID> {
    @Query("select t from Tarjeta t where t.idLiga = :liga "
            + "and (:estado = '' or t.estado = :estado) "
            + "and (:descripcion = '' or lower(t.descripcion) like lower(concat('%', :descripcion, '%'))) "
            + "order by t.descripcion")
    List<Tarjeta> buscar(@Param("liga") UUID liga,
                         @Param("descripcion") String descripcion,
                         @Param("estado") String estado);
}

interface PartidoArbitroRepository extends JpaRepository<PartidoArbitro, UUID> {
    List<PartidoArbitro> findByIdPartido(UUID idPartido);
}

interface GolJugadorRepository extends JpaRepository<GolJugador, UUID> {
    List<GolJugador> findByIdPartido(UUID idPartido);

    @Query("select g from GolJugador g where g.estado = 'ACTIVO' and g.idPartido in "
            + "(select p.id from Partido p where p.idCategoria = :categoria and p.estado = 'ACTIVO')")
    List<GolJugador> activosDeCategoria(@Param("categoria") UUID categoria);
}

interface TarjetaJugadorRepository extends JpaRepository<TarjetaJugador, UUID> {
    List<TarjetaJugador> findByIdPartido(UUID idPartido);

    @Query("select t from TarjetaJugador t where t.estado = 'ACTIVO' and t.idPartido in "
            + "(select p.id from Partido p where p.idCategoria = :categoria and p.estado = 'ACTIVO')")
    List<TarjetaJugador> activasDeCategoria(@Param("categoria") UUID categoria);
}

interface PartidoSancionRepository extends JpaRepository<PartidoSancion, UUID> {
    List<PartidoSancion> findByIdPartido(UUID idPartido);

    @Query("select s from PartidoSancion s where s.estado = 'ACTIVO' and s.idPartido in "
            + "(select p.id from Partido p where p.idCategoria = :categoria and p.estado = 'ACTIVO')")
    List<PartidoSancion> activasDeCategoria(@Param("categoria") UUID categoria);
}

interface PartidoSancionDetalleRepository extends JpaRepository<PartidoSancionDetalle, UUID> {
    List<PartidoSancionDetalle> findByIdPartidoSancion(UUID idPartidoSancion);
}

interface PartidoCobroRepository extends JpaRepository<PartidoCobro, UUID> {
    List<PartidoCobro> findByIdPartido(UUID idPartido);

    @Query("select c from PartidoCobro c where c.estado = 'ACTIVO' and c.idPartido in :partidos")
    List<PartidoCobro> activosDePartidos(@Param("partidos") Collection<UUID> partidos);
}

interface PartidoCobroDetalleRepository extends JpaRepository<PartidoCobroDetalle, UUID> {
    List<PartidoCobroDetalle> findByIdPartidoCobro(UUID idPartidoCobro);

    List<PartidoCobroDetalle> findByIdTarjetaJugador(UUID idTarjetaJugador);

    List<PartidoCobroDetalle> findByIdPartidoSancionDetalle(UUID idPartidoSancionDetalle);

    @Query("select d from PartidoCobroDetalle d where d.estado = 'ACTIVO' and d.tipo in ('TARJETA', 'SANCION')")
    List<PartidoCobroDetalle> cargosActivos();
}

interface CriterioDesempateGoleadorRepository extends JpaRepository<CriterioDesempateGoleador, UUID> {
    List<CriterioDesempateGoleador> findByIdEdicionAndEstadoOrderByOrdenAsc(UUID idEdicion, String estado);
}

interface CriterioDesempateEquipoRepository extends JpaRepository<CriterioDesempateEquipo, UUID> {
    List<CriterioDesempateEquipo> findByIdEdicionAndEstadoOrderByOrdenAsc(UUID idEdicion, String estado);
}

interface CanchaRepository extends JpaRepository<Cancha, UUID> {
    @Query("select c from Cancha c where c.idLiga = :liga and c.estado <> 'ELIMINADO' "
            + "and (:estado = '' or c.estado = :estado) "
            + "and (:descripcion = '' or lower(c.descripcion) like :descripcion) "
            + "order by c.descripcion")
    List<Cancha> buscar(@Param("liga") UUID liga, @Param("descripcion") String descripcion, @Param("estado") String estado);
}
