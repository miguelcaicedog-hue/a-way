package com.away.core.competition;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

class LeagueRequest {
    @NotBlank
    private String name;
    @NotNull
    private LocalDate validFrom;
    @NotNull
    private LocalDate validUntil;
    private boolean active = true;

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
}

class TeamRequest {
    @NotNull
    private UUID leagueId;
    @NotBlank
    private String name;

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
}

class LeagueView {
    private final UUID id;
    private final String name;
    private final LocalDate validFrom;
    private final LocalDate validUntil;
    private final boolean active;

    LeagueView(UUID id, String name, LocalDate validFrom, LocalDate validUntil, boolean active) {
        this.id = id;
        this.name = name;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.active = active;
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

    CategoryView(UUID id, UUID editionId, String editionName, UUID leagueId, String leagueName, String name, int maxPlayers) {
        this.id = id;
        this.editionId = editionId;
        this.editionName = editionName;
        this.leagueId = leagueId;
        this.leagueName = leagueName;
        this.name = name;
        this.maxPlayers = maxPlayers;
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
}

class TeamView {
    private final UUID id;
    private final UUID leagueId;
    private final String leagueName;
    private final String name;

    TeamView(UUID id, UUID leagueId, String leagueName, String name) {
        this.id = id;
        this.leagueId = leagueId;
        this.leagueName = leagueName;
        this.name = name;
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
    @Min(1)
    private Integer numeroCamisa;
    @NotNull
    private Integer rotationDegrees;
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
    private final boolean hasPhoto;

    PlayerView(UUID id, UUID teamEntryId, UUID teamId, String teamName, UUID categoryId,
               String name, String alias, String cedula, int numeroCamisa, int rotationDegrees, boolean hasPhoto) {
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

    public boolean isHasPhoto() {
        return hasPhoto;
    }
}
