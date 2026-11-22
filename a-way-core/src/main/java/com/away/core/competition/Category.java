package com.away.core.competition;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "category")
public class Category {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "edition_id")
    private Edition edition;

    @Column(nullable = false)
    private String name;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "cantidad_grupos", nullable = false)
    private int cantidadGrupos;

    @Column(name = "equipos_por_grupo")
    private Integer equiposPorGrupo;

    @Column(name = "cantidad_fases", nullable = false)
    private int cantidadFases;

    @Column(name = "valor_pago_partido", nullable = false)
    private BigDecimal valorPagoPartido;

    @Column(name = "valor_cobro_equipo", nullable = false)
    private BigDecimal valorCobroEquipo;

    @Column(name = "valor_pago_sin_pito", nullable = false)
    private BigDecimal valorPagoSinPito;

    @Column(nullable = false, length = 20)
    private String estado;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Edition getEdition() {
        return edition;
    }

    public void setEdition(Edition edition) {
        this.edition = edition;
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
