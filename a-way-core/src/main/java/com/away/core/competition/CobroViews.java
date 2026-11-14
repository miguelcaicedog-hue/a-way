package com.away.core.competition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CobroViews {

    private CobroViews() {
    }

    public static class EquipoView {
        private final UUID id;
        private final String nombre;

        public EquipoView(UUID id, String nombre) {
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

    public static class CargoView {
        private final UUID id;
        private final UUID jugadorId;
        private final int numero;
        private final String alias;
        private final boolean hasFoto;
        private final int rotacion;
        private final String jornada;
        private final String detalle;
        private final BigDecimal costo;
        private final boolean marcado;

        public CargoView(UUID id, UUID jugadorId, int numero, String alias, boolean hasFoto, int rotacion,
                         String jornada, String detalle, BigDecimal costo, boolean marcado) {
            this.id = id;
            this.jugadorId = jugadorId;
            this.numero = numero;
            this.alias = alias;
            this.hasFoto = hasFoto;
            this.rotacion = rotacion;
            this.jornada = jornada;
            this.detalle = detalle;
            this.costo = costo;
            this.marcado = marcado;
        }

        public UUID getId() {
            return id;
        }

        public UUID getJugadorId() {
            return jugadorId;
        }

        public int getNumero() {
            return numero;
        }

        public String getAlias() {
            return alias;
        }

        public boolean isHasFoto() {
            return hasFoto;
        }

        public int getRotacion() {
            return rotacion;
        }

        public String getJornada() {
            return jornada;
        }

        public String getDetalle() {
            return detalle;
        }

        public BigDecimal getCosto() {
            return costo;
        }

        public boolean isMarcado() {
            return marcado;
        }
    }

    public static class CobroView {
        private final String local;
        private final String visita;
        private final EquipoView equipoLocal;
        private final EquipoView equipoVisita;
        private final String jornadaNombre;
        private final LocalDate fecha;
        private final String hora;
        private final String canchaNombre;
        private final boolean editable;
        private final UUID equipoId;
        private final String equipoNombre;
        private final BigDecimal arbitraje;
        private final List<CargoView> tarjetas;
        private final List<CargoView> sanciones;
        private final String observacion;
        private final BigDecimal total;
        private final BigDecimal valorCobroEfectivo;
        private final BigDecimal valorCobroTransferencia;

        public CobroView(String local, String visita, EquipoView equipoLocal, EquipoView equipoVisita,
                         String jornadaNombre, LocalDate fecha, String hora, String canchaNombre, boolean editable,
                         UUID equipoId, String equipoNombre, BigDecimal arbitraje, List<CargoView> tarjetas,
                         List<CargoView> sanciones, String observacion, BigDecimal total,
                         BigDecimal valorCobroEfectivo, BigDecimal valorCobroTransferencia) {
            this.local = local;
            this.visita = visita;
            this.equipoLocal = equipoLocal;
            this.equipoVisita = equipoVisita;
            this.jornadaNombre = jornadaNombre;
            this.fecha = fecha;
            this.hora = hora;
            this.canchaNombre = canchaNombre;
            this.editable = editable;
            this.equipoId = equipoId;
            this.equipoNombre = equipoNombre;
            this.arbitraje = arbitraje;
            this.tarjetas = tarjetas;
            this.sanciones = sanciones;
            this.observacion = observacion;
            this.total = total;
            this.valorCobroEfectivo = valorCobroEfectivo;
            this.valorCobroTransferencia = valorCobroTransferencia;
        }

        public String getLocal() {
            return local;
        }

        public String getVisita() {
            return visita;
        }

        public EquipoView getEquipoLocal() {
            return equipoLocal;
        }

        public EquipoView getEquipoVisita() {
            return equipoVisita;
        }

        public String getJornadaNombre() {
            return jornadaNombre;
        }

        public LocalDate getFecha() {
            return fecha;
        }

        public String getHora() {
            return hora;
        }

        public String getCanchaNombre() {
            return canchaNombre;
        }

        public boolean isEditable() {
            return editable;
        }

        public UUID getEquipoId() {
            return equipoId;
        }

        public String getEquipoNombre() {
            return equipoNombre;
        }

        public BigDecimal getArbitraje() {
            return arbitraje;
        }

        public List<CargoView> getTarjetas() {
            return tarjetas;
        }

        public List<CargoView> getSanciones() {
            return sanciones;
        }

        public String getObservacion() {
            return observacion;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public BigDecimal getValorCobroEfectivo() {
            return valorCobroEfectivo;
        }

        public BigDecimal getValorCobroTransferencia() {
            return valorCobroTransferencia;
        }
    }

    public static class CobroGuardarRequest {
        private UUID equipoId;
        private List<UUID> tarjetaIds;
        private List<UUID> sancionIds;
        private String observacion;
        private BigDecimal valorCobroEfectivo;
        private BigDecimal valorCobroTransferencia;

        public UUID getEquipoId() {
            return equipoId;
        }

        public void setEquipoId(UUID equipoId) {
            this.equipoId = equipoId;
        }

        public List<UUID> getTarjetaIds() {
            return tarjetaIds;
        }

        public void setTarjetaIds(List<UUID> tarjetaIds) {
            this.tarjetaIds = tarjetaIds;
        }

        public List<UUID> getSancionIds() {
            return sancionIds;
        }

        public void setSancionIds(List<UUID> sancionIds) {
            this.sancionIds = sancionIds;
        }

        public String getObservacion() {
            return observacion;
        }

        public void setObservacion(String observacion) {
            this.observacion = observacion;
        }

        public BigDecimal getValorCobroEfectivo() {
            return valorCobroEfectivo;
        }

        public void setValorCobroEfectivo(BigDecimal valorCobroEfectivo) {
            this.valorCobroEfectivo = valorCobroEfectivo;
        }

        public BigDecimal getValorCobroTransferencia() {
            return valorCobroTransferencia;
        }

        public void setValorCobroTransferencia(BigDecimal valorCobroTransferencia) {
            this.valorCobroTransferencia = valorCobroTransferencia;
        }
    }
}
