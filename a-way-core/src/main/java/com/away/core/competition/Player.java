package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "player")
public class Player {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_entry_id")
    private TeamEntry teamEntry;

    @Column(nullable = false)
    private String name;

    private String alias;

    @Column(nullable = false)
    private String cedula;

    @Column(name = "numero_camisa", nullable = false)
    private int numeroCamisa;

    @Column(name = "photo")
    private byte[] photo;

    @Column(name = "photo_content_type")
    private String photoContentType;

    @Column(name = "rotation_degrees", nullable = false)
    private short rotationDegrees;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private boolean habilitado = false;

    @Column(nullable = false)
    private boolean reemplazo = false;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TeamEntry getTeamEntry() {
        return teamEntry;
    }

    public void setTeamEntry(TeamEntry teamEntry) {
        this.teamEntry = teamEntry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Mayusculas.de(name);
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = Mayusculas.de(alias);
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public int getNumeroCamisa() {
        return numeroCamisa;
    }

    public void setNumeroCamisa(int numeroCamisa) {
        this.numeroCamisa = numeroCamisa;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }

    public short getRotationDegrees() {
        return rotationDegrees;
    }

    public void setRotationDegrees(short rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public boolean isReemplazo() {
        return reemplazo;
    }

    public void setReemplazo(boolean reemplazo) {
        this.reemplazo = reemplazo;
    }
}
