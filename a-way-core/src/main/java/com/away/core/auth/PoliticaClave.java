package com.away.core.auth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "politica_clave")
public class PoliticaClave {

    @Id
    @Column(name = "id_politica_clave")
    private UUID id;

    @Column(name = "longitud_minima", nullable = false)
    private int longitudMinima;

    @Column(name = "requiere_letra", nullable = false)
    private boolean requiereLetra;

    @Column(name = "requiere_numero", nullable = false)
    private boolean requiereNumero;

    @Column(name = "requiere_especial", nullable = false)
    private boolean requiereEspecial;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_modificacion", nullable = false)
    private OffsetDateTime fechaModificacion;

    @Column(name = "usuario_creacion", nullable = false, updatable = false, length = 80)
    private String usuarioCreacion;

    @Column(name = "usuario_modificacion", nullable = false, length = 80)
    private String usuarioModificacion;

    @Column(nullable = false, length = 20)
    private String estado;

    public int getLongitudMinima() {
        return longitudMinima;
    }

    public boolean isRequiereLetra() {
        return requiereLetra;
    }

    public boolean isRequiereNumero() {
        return requiereNumero;
    }

    public boolean isRequiereEspecial() {
        return requiereEspecial;
    }
}
