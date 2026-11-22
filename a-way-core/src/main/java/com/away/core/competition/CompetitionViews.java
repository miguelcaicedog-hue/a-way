package com.away.core.competition;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

class LeagueRequest {
    @NotBlank
    private String name;
    @NotNull
    private LocalDate validFrom;
    @NotNull
    private LocalDate validUntil;
    private boolean active = true;
    private String logoBase64;
    private String logoContentType;
    private boolean clearLogo;
    private String imagenPlantilla;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public String getLogoContentType() {
        return logoContentType;
    }

    public void setLogoContentType(String logoContentType) {
        this.logoContentType = logoContentType;
    }

    public boolean isClearLogo() {
        return clearLogo;
    }

    public void setClearLogo(boolean clearLogo) {
        this.clearLogo = clearLogo;
    }

    public String getImagenPlantilla() {
        return imagenPlantilla;
    }

    public void setImagenPlantilla(String imagenPlantilla) {
        this.imagenPlantilla = imagenPlantilla;
    }
}

class EditionRequest {
    @NotNull
    private UUID leagueId;
    @NotBlank
    private String name;
    @NotNull
    private LocalDate startsOn;
    @NotNull
    private LocalDate endsOn;

    public UUID getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(UUID leagueId) {
        this.leagueId = leagueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public void setStartsOn(LocalDate startsOn) {
        this.startsOn = startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public void setEndsOn(LocalDate endsOn) {
        this.endsOn = endsOn;
    }
}

class CategoryRequest {
    @NotNull
    private UUID editionId;
    @NotBlank
    private String name;
    @Min(1)
    private int maxPlayers;
    @Min(1)
    private int cantidadGrupos;
    @Min(1)
    private Integer equiposPorGrupo;
    @Min(1)
    private int cantidadFases;
    private BigDecimal valorPagoPartido;
    private BigDecimal valorCobroEquipo;
    private BigDecimal valorPagoSinPito;
    private String estado;

    public UUID getEditionId() {
        return editionId;
    }

    public void setEditionId(UUID editionId) {
        this.editionId = editionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getCantidadGrupos() {
        return cantidadGrupos;
    }

    public void setCantidadGrupos(int cantidadGrupos) {
        this.cantidadGrupos = cantidadGrupos;
    }

    public Integer getEquiposPorGrupo() {
        return equiposPorGrupo;
    }

    public void setEquiposPorGrupo(Integer equiposPorGrupo) {
        this.equiposPorGrupo = equiposPorGrupo;
    }

    public int getCantidadFases() {
        return cantidadFases;
    }

    public void setCantidadFases(int cantidadFases) {
        this.cantidadFases = cantidadFases;
    }

    public BigDecimal getValorPagoPartido() {
        return valorPagoPartido;
    }

    public void setValorPagoPartido(BigDecimal valorPagoPartido) {
        this.valorPagoPartido = valorPagoPartido;
    }

    public BigDecimal getValorCobroEquipo() {
        return valorCobroEquipo;
    }

    public void setValorCobroEquipo(BigDecimal valorCobroEquipo) {
        this.valorCobroEquipo = valorCobroEquipo;
    }

    public BigDecimal getValorPagoSinPito() {
        return valorPagoSinPito;
    }

    public void setValorPagoSinPito(BigDecimal valorPagoSinPito) {
        this.valorPagoSinPito = valorPagoSinPito;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}

class TeamRequest {
    @NotNull
    private UUID leagueId;
    @NotBlank
    private String name;
    @Size(max = 160)
    private String nombreRepresentante;
    @Size(max = 30)
    private String numeroTelefono;
    @Size(max = 20)
    private String estado;
    private String logoBase64;
    private String logoContentType;
    private boolean clearLogo;
    private UUID categoryId;
    private UUID categoryIdActual;

    public UUID getLeagueId() {
        return leagueId;
    }

    public void setLeagueId(UUID leagueId) {
        this.leagueId = leagueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNombreRepresentante() {
        return nombreRepresentante;
    }

    public void setNombreRepresentante(String nombreRepresentante) {
        this.nombreRepresentante = nombreRepresentante;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
    }

    public void setNumeroTelefono(String numeroTelefono) {
        this.numeroTelefono = numeroTelefono;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public String getLogoContentType() {
        return logoContentType;
    }

    public void setLogoContentType(String logoContentType) {
        this.logoContentType = logoContentType;
    }

    public boolean isClearLogo() {
        return clearLogo;
    }

    public void setClearLogo(boolean clearLogo) {
        this.clearLogo = clearLogo;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getCategoryIdActual() {
        return categoryIdActual;
    }

    public void setCategoryIdActual(UUID categoryIdActual) {
        this.categoryIdActual = categoryIdActual;
    }
}

class LeagueView {
    private final UUID id;
    private final String name;
    private final LocalDate validFrom;
    private final LocalDate validUntil;
    private final boolean active;
    private final boolean hasLogo;
    private final String imagenPlantilla;

    LeagueView(UUID id, String name, LocalDate validFrom, LocalDate validUntil, boolean active, boolean hasLogo,
               String imagenPlantilla) {
        this.id = id;
        this.name = name;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.active = active;
        this.hasLogo = hasLogo;
        this.imagenPlantilla = imagenPlantilla;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isHasLogo() {
        return hasLogo;
    }

    public String getImagenPlantilla() {
        return imagenPlantilla;
    }
}

class EditionView {
    private final UUID id;
    private final UUID leagueId;
    private final String leagueName;
    private final String name;
    private final LocalDate startsOn;
    private final LocalDate endsOn;

    EditionView(UUID id, UUID leagueId, String leagueName, String name, LocalDate startsOn, LocalDate endsOn) {
        this.id = id;
        this.leagueId = leagueId;
        this.leagueName = leagueName;
        this.name = name;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeagueId() {
        return leagueId;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }
}

class CategoryView {
    private final UUID id;
    private final UUID editionId;
    private final String editionName;
    private final UUID leagueId;
    private final String leagueName;
    private final String name;
    private final int maxPlayers;
    private final int cantidadGrupos;
    private final Integer equiposPorGrupo;
    private final int cantidadFases;
    private final BigDecimal valorPagoPartido;
    private final BigDecimal valorCobroEquipo;
    private final BigDecimal valorPagoSinPito;
    private final String estado;

    CategoryView(UUID id, UUID editionId, String editionName, UUID leagueId, String leagueName, String name, int maxPlayers,
                 int cantidadGrupos, Integer equiposPorGrupo, int cantidadFases, BigDecimal valorPagoPartido,
                 BigDecimal valorCobroEquipo, BigDecimal valorPagoSinPito, String estado) {
        this.id = id;
        this.editionId = editionId;
        this.editionName = editionName;
        this.leagueId = leagueId;
        this.leagueName = leagueName;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.cantidadGrupos = cantidadGrupos;
        this.equiposPorGrupo = equiposPorGrupo;
        this.cantidadFases = cantidadFases;
        this.valorPagoPartido = valorPagoPartido;
        this.valorCobroEquipo = valorCobroEquipo;
        this.valorPagoSinPito = valorPagoSinPito;
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEditionId() {
        return editionId;
    }

    public String getEditionName() {
        return editionName;
    }

    public UUID getLeagueId() {
        return leagueId;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public String getName() {
        return name;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getCantidadGrupos() {
        return cantidadGrupos;
    }

    public Integer getEquiposPorGrupo() {
        return equiposPorGrupo;
    }

    public int getCantidadFases() {
        return cantidadFases;
    }

    public BigDecimal getValorPagoPartido() {
        return valorPagoPartido;
    }

    public BigDecimal getValorCobroEquipo() {
        return valorCobroEquipo;
    }

    public BigDecimal getValorPagoSinPito() {
        return valorPagoSinPito;
    }

    public String getEstado() {
        return estado;
    }
}

class CategoriaResumen {
    private final UUID id;
    private final String nombre;

    CategoriaResumen(UUID id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}

class TeamView {
    private final UUID id;
    private final UUID leagueId;
    private final String leagueName;
    private final String name;
    private final String nombreRepresentante;
    private final String numeroTelefono;
    private final String estado;
    private final boolean hasLogo;
    private final long totalJugadores;
    private final List<CategoriaResumen> categorias;

    TeamView(UUID id, UUID leagueId, String leagueName, String name, String nombreRepresentante, String numeroTelefono, String estado, boolean hasLogo, long totalJugadores, List<CategoriaResumen> categorias) {
        this.id = id;
        this.leagueId = leagueId;
        this.leagueName = leagueName;
        this.name = name;
        this.nombreRepresentante = nombreRepresentante;
        this.numeroTelefono = numeroTelefono;
        this.estado = estado;
        this.hasLogo = hasLogo;
        this.totalJugadores = totalJugadores;
        this.categorias = categorias;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeagueId() {
        return leagueId;
    }

    public String getLeagueName() {
        return leagueName;
    }

    public String getName() {
        return name;
    }

    public String getNombreRepresentante() {
        return nombreRepresentante;
    }

    public String getNumeroTelefono() {
        return numeroTelefono;
    }

    public String getEstado() {
        return estado;
    }

    public boolean isHasLogo() {
        return hasLogo;
    }

    public long getTotalJugadores() {
        return totalJugadores;
    }

    public List<CategoriaResumen> getCategorias() {
        return categorias;
    }
}

class TeamEntryRequest {
    @NotNull
    private UUID teamId;
    @NotNull
    private UUID categoryId;

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
}

class TeamEntryView {
    private final UUID id;
    private final UUID teamId;
    private final String teamName;
    private final UUID categoryId;
    private final String categoryName;
    private final UUID leagueId;

    TeamEntryView(UUID id, UUID teamId, String teamName, UUID categoryId, String categoryName, UUID leagueId) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.leagueId = leagueId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public UUID getLeagueId() {
        return leagueId;
    }
}

class PlayerRequest {
    @NotNull
    private UUID teamEntryId;
    @NotBlank
    @Size(max = 160)
    private String name;
    @Size(max = 80)
    private String alias;
    @NotBlank
    @Size(max = 30)
    private String cedula;
    @NotNull
    @Min(0)
    private Integer numeroCamisa;
    @NotNull
    private Integer rotationDegrees;
    private String estado;
    private String photoBase64;
    private String photoContentType;
    private boolean clearPhoto;

    public UUID getTeamEntryId() {
        return teamEntryId;
    }

    public void setTeamEntryId(UUID teamEntryId) {
        this.teamEntryId = teamEntryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public Integer getNumeroCamisa() {
        return numeroCamisa;
    }

    public void setNumeroCamisa(Integer numeroCamisa) {
        this.numeroCamisa = numeroCamisa;
    }

    public Integer getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(Integer rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public boolean isClearPhoto() {
        return clearPhoto;
    }

    public void setClearPhoto(boolean clearPhoto) {
        this.clearPhoto = clearPhoto;
    }
}

class HabilitadoRequest {
    @NotNull
    private Boolean habilitado;

    public Boolean getHabilitado() {
        return habilitado;
    }

    public void setHabilitado(Boolean habilitado) {
        this.habilitado = habilitado;
    }
}

class PlayerView {
    private final UUID id;
    private final UUID teamEntryId;
    private final UUID teamId;
    private final String teamName;
    private final UUID categoryId;
    private final String name;
    private final String alias;
    private final String cedula;
    private final int numeroCamisa;
    private final int rotationDegrees;
    private final String estado;
    private final boolean habilitado;
    private final boolean hasPhoto;

    PlayerView(UUID id, UUID teamEntryId, UUID teamId, String teamName, UUID categoryId,
               String name, String alias, String cedula, int numeroCamisa, int rotationDegrees, String estado,
               boolean habilitado, boolean hasPhoto) {
        this.id = id;
        this.teamEntryId = teamEntryId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.categoryId = categoryId;
        this.name = name;
        this.alias = alias;
        this.cedula = cedula;
        this.numeroCamisa = numeroCamisa;
        this.rotationDegrees = rotationDegrees;
        this.estado = estado;
        this.habilitado = habilitado;
        this.hasPhoto = hasPhoto;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTeamEntryId() {
        return teamEntryId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public String getCedula() {
        return cedula;
    }

    public int getNumeroCamisa() {
        return numeroCamisa;
    }

    public int getRotationDegrees() {
        return rotationDegrees;
    }

    public String getEstado() {
        return estado;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public boolean isHasPhoto() {
        return hasPhoto;
    }
}
